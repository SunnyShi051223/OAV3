package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.attendance.dto.ApplicationCreateDTO;
import com.oa.attendance.dto.ApprovalDecisionDTO;
import com.oa.attendance.entity.AppApplication;
import com.oa.attendance.entity.AttRecord;
import com.oa.attendance.entity.AttRule;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysDepartment;
import com.oa.attendance.entity.SysPosition;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.exception.BusinessException;
import com.oa.attendance.mapper.AppApplicationMapper;
import com.oa.attendance.mapper.AttRecordMapper;
import com.oa.attendance.mapper.AttRuleMapper;
import com.oa.attendance.mapper.SysDepartmentMapper;
import com.oa.attendance.mapper.SysPositionMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.service.IAttendanceApprovalSyncService;
import com.oa.attendance.service.IApplicationService;
import com.oa.attendance.vo.ApplicationTypeVO;
import com.oa.attendance.vo.ApplicationVO;
import com.oa.attendance.vo.ApprovalHistoryVO;
import com.oa.attendance.vo.MakeupRecordOptionVO;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImpl implements IApplicationService {

    private static final String PROCESS_KEY = "applicationApproval";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String TYPE_MAKEUP = "MAKEUP";

    @Autowired
    private AppApplicationMapper applicationMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysPositionMapper positionMapper;

    @Autowired
    private SysDepartmentMapper departmentMapper;

    @Autowired
    private AttRecordMapper recordMapper;

    @Autowired
    private AttRuleMapper ruleMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private IAttendanceApprovalSyncService attendanceApprovalSyncService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ApprovalReminderScheduler reminderScheduler;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public Result<ApplicationVO> submit(ApplicationCreateDTO dto) {
        SysUser current = requireCurrentUser();
        SysUser applicant = userMapper.getUserDetailById(current.getUserId());
        if (applicant == null || applicant.getDeptId() == null) {
            throw new BusinessException(400, "请先完善所属部门后再提交申请");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }

        Map<String, String> typeLabels = loadTypeLabels();
        if (!typeLabels.containsKey(dto.getApplicationType())) {
            throw new BusinessException(400, "申请类型不存在或已停用");
        }
        validateApplicationScope(dto, applicant);

        LocalDateTime now = LocalDateTime.now();
        AppApplication application = new AppApplication();
        BeanUtils.copyProperties(dto, application, "attachmentUrls");
        application.setApplicantId(applicant.getUserId());
        application.setApplicantName(applicant.getRealName());
        application.setApplicantDeptId(applicant.getDeptId());
        application.setApplicantDeptName(applicant.getDeptName());
        application.setAttendanceRecordId(dto.getAttendanceRecordId());
        application.setAttachmentUrls(writeAttachments(dto.getAttachmentUrls()));
        application.setStatus(STATUS_PROCESSING);
        application.setCreateTime(now);
        application.setUpdateTime(now);
        applicationMapper.insert(application);

        Map<String, Object> variables = new HashMap<>();
        variables.put("applicationId", application.getApplicationId());
        variables.put("applicantId", applicant.getUserId());
        variables.put("applicantName", applicant.getRealName());
        variables.put("applicantDeptId", applicant.getDeptId());
        variables.put("applicationType", dto.getApplicationType());

        long durationHours = Duration.between(dto.getStartTime(), dto.getEndTime()).toHours();
        boolean longDuration = durationHours > 24;
        variables.put("longDuration", longDuration);

        String level1Approver = findApproverByLevel(applicant, 1);
        variables.put("level1Approver", level1Approver);
        if (longDuration) {
            String level2Approver = findApproverByLevel(applicant, 2);
            variables.put("level2Approver", level2Approver);
        }

        Authentication.setAuthenticatedUserId(applicant.getUsername());
        try {
            ProcessInstance process = runtimeService.startProcessInstanceByKey(
                    PROCESS_KEY, String.valueOf(application.getApplicationId()), variables);
            application.setProcessInstanceId(process.getProcessInstanceId());
            application.setUpdateTime(LocalDateTime.now());
            applicationMapper.updateById(application);
        } finally {
            Authentication.setAuthenticatedUserId(null);
        }

        String typeLabel = typeLabels.getOrDefault(dto.getApplicationType(), dto.getApplicationType());
        Long level1UserId = resolveUserId(level1Approver);
        if (level1UserId != null) {
            notificationService.sendToUser(level1UserId,
                    String.format("%s 提交了%s申请，请审批", applicant.getRealName(), typeLabel));
        }

        auditService.log(applicant.getUserId(), applicant.getUsername(),
                "SUBMIT", "application", application.getApplicationId(),
                String.format("提交了%s申请", typeLabel));

        // 紧急请假/加班：开始时间已到，立即发邮件提醒审批人
        if (("LEAVE".equals(dto.getApplicationType()) || "OVERTIME".equals(dto.getApplicationType()))
                && !dto.getStartTime().isAfter(LocalDateTime.now())) {
            reminderScheduler.sendUrgentReminder(application, level1Approver);
        }

        return Result.success("申请已提交", toVO(application, true, null));
    }

    @Override
    public Result<List<ApplicationVO>> listMine() {
        SysUser current = requireCurrentUser();
        autoCancelExpired();
        QueryWrapper<AppApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("applicant_id", current.getUserId()).orderByDesc("create_time");
        return Result.success("查询成功", toVOList(applicationMapper.selectList(wrapper), null));
    }

    @Override
    public Result<ApplicationVO> getDetail(Long applicationId) {
        AppApplication application = requireApplication(applicationId);
        SysUser current = requireCurrentUser();
        boolean own = current.getUserId().equals(application.getApplicantId());
        boolean manageable = dataScopeService.hasFullDataAccess()
                || (dataScopeService.hasDepartmentDataAccess()
                && current.getDeptId() != null
                && current.getDeptId().equals(application.getApplicantDeptId()));
        if (!own && !manageable) {
            throw new BusinessException(403, "无权查看该申请");
        }
        return Result.success("查询成功", toVO(application, true, null));
    }

    @Override
    public Result<List<ApplicationTypeVO>> listApplicationTypes() {
        return Result.success("查询成功", applicationMapper.selectEnabledApplicationTypes());
    }

    @Override
    public Result<List<MakeupRecordOptionVO>> listMakeupRecordOptions() {
        SysUser current = requireCurrentUser();
        List<MakeupRecordOptionVO> options = recordMapper.selectCorrectableRecords(current.getUserId()).stream()
                .map(this::toMakeupRecordOption)
                .collect(Collectors.toList());
        return Result.success("查询成功", options);
    }

    @Override
    public Result<List<ApplicationVO>> listPendingTasks() {
        autoCancelExpired();
        SysUser current = requireApprover();
        List<Task> tasks;
        if (dataScopeService.hasFullDataAccess()) {
            tasks = taskService.createTaskQuery()
                    .active()
                    .orderByTaskCreateTime().desc()
                    .list();
        } else {
            tasks = taskService.createTaskQuery()
                    .taskAssignee(current.getUsername())
                    .active()
                    .orderByTaskCreateTime().desc()
                    .list();
        }

        Map<String, String> typeLabels = loadTypeLabels();
        List<ApplicationVO> result = new ArrayList<>();
        for (Task task : tasks) {
            AppApplication application = findByProcessInstanceId(task.getProcessInstanceId());
            if (application != null) {
                result.add(toVO(application, false, task, typeLabels));
            }
        }
        return Result.success("查询成功", result);
    }

    @Override
    public Result<List<ApplicationVO>> listHandledTasks() {
        SysUser current = requireApprover();
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(current.getUsername())
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        Map<String, String> typeLabels = loadTypeLabels();
        Map<Long, ApplicationVO> distinct = new LinkedHashMap<>();
        for (HistoricTaskInstance task : tasks) {
            AppApplication application = findByProcessInstanceId(task.getProcessInstanceId());
            if (application != null && !distinct.containsKey(application.getApplicationId())) {
                distinct.put(application.getApplicationId(), toVO(application, true, null, typeLabels));
            }
        }
        return Result.success("查询成功", new ArrayList<>(distinct.values()));
    }

    @Override
    @Transactional
    public Result<ApplicationVO> approve(String taskId, ApprovalDecisionDTO dto) {
        return completeTask(taskId, dto, true);
    }

    @Override
    @Transactional
    public Result<ApplicationVO> reject(String taskId, ApprovalDecisionDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getComment())) {
            throw new BusinessException(400, "驳回申请时必须填写审批意见");
        }
        return completeTask(taskId, dto, false);
    }

    @Override
    public Result<List<ApplicationVO>> listAll() {
        SysUser current = requireCurrentUser();
        if (!dataScopeService.hasFullDataAccess() && !dataScopeService.hasDepartmentDataAccess()) {
            throw new BusinessException(403, "当前用户没有查看全部审批的权限");
        }
        QueryWrapper<AppApplication> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        if (dataScopeService.hasDepartmentDataAccess() && !dataScopeService.hasFullDataAccess()) {
            wrapper.eq("applicant_dept_id", current.getDeptId());
        }
        return Result.success("查询成功", toVOList(applicationMapper.selectList(wrapper), null));
    }

    @Override
    @Transactional
    public Result<ApplicationVO> cancel(Long applicationId) {
        SysUser current = requireCurrentUser();
        AppApplication application = requireApplication(applicationId);
        if (!current.getUserId().equals(application.getApplicantId())) {
            throw new BusinessException(403, "只能撤回本人提交的申请");
        }
        if (!STATUS_PROCESSING.equals(application.getStatus())) {
            throw new BusinessException(409, "只有审批中的申请可以撤回");
        }

        ProcessInstance process = runtimeService.createProcessInstanceQuery()
                .processInstanceId(application.getProcessInstanceId())
                .singleResult();
        if (process == null) {
            throw new BusinessException(409, "流程已结束，无法撤回");
        }
        Task task = taskService.createTaskQuery()
                .processInstanceId(application.getProcessInstanceId())
                .active()
                .singleResult();
        if (task != null) {
            taskService.setVariableLocal(task.getId(), "decision", STATUS_CANCELED);
            taskService.setVariableLocal(task.getId(), "approvalComment", "申请人撤回");
            taskService.setVariableLocal(task.getId(), "approverName", application.getApplicantName());
        }

        try {
            runtimeService.deleteProcessInstance(application.getProcessInstanceId(), "申请人撤回");
        } catch (FlowableOptimisticLockingException e) {
            throw new BusinessException(409, "申请状态已变化，请刷新后重试");
        }

        application.setStatus(STATUS_CANCELED);
        application.setCompleteTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        auditService.log(current.getUserId(), current.getUsername(),
                "CANCEL", "application", application.getApplicationId(),
                "撤回了申请");

        return Result.success("申请已撤回", toVO(application, true, null));
    }

    private Result<ApplicationVO> completeTask(String taskId, ApprovalDecisionDTO dto, boolean approved) {
        SysUser approver = requireApprover();
        Task task = taskService.createTaskQuery().taskId(taskId).active().singleResult();
        if (task == null) {
            throw new BusinessException(409, "审批任务不存在或已处理");
        }

        AppApplication application = findByProcessInstanceId(task.getProcessInstanceId());
        if (application == null) {
            throw new BusinessException("审批业务数据不存在");
        }
        ensureCanHandle(application, approver);

        if (StringUtils.hasText(task.getAssignee()) && !approver.getUsername().equals(task.getAssignee())) {
            if (dataScopeService.hasFullDataAccess()) {
                taskService.unclaim(taskId);
                taskService.claim(taskId, approver.getUsername());
            } else {
                throw new BusinessException(409, "该任务已由其他审批人领取");
            }
        }
        String decision = approved ? STATUS_APPROVED : STATUS_REJECTED;
        String comment = dto == null ? null : dto.getComment();
        try {
            if (!StringUtils.hasText(task.getAssignee())) {
                taskService.claim(taskId, approver.getUsername());
            }
            taskService.setVariableLocal(taskId, "decision", decision);
            taskService.setVariableLocal(taskId, "approvalComment", comment == null ? "" : comment.trim());
            taskService.setVariableLocal(taskId, "approverName", approver.getRealName());
            Map<String, Object> processVariables = new HashMap<>();
            processVariables.put("approved", approved);
            processVariables.put("decision", decision);
            taskService.complete(taskId, processVariables);
        } catch (FlowableOptimisticLockingException e) {
            throw new BusinessException(409, "该审批已被其他人处理，请刷新列表");
        } catch (FlowableException e) {
            if (taskService.createTaskQuery().taskId(taskId).active().singleResult() == null) {
                throw new BusinessException(409, "该审批已被其他人处理，请刷新列表");
            }
            throw e;
        }

        boolean processEnded = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult() == null;

        if (!approved) {
            application.setStatus(STATUS_REJECTED);
            application.setCompleteTime(LocalDateTime.now());
            notificationService.sendToUser(application.getApplicantId(),
                    String.format("您的%s申请已被 %s 驳回%s",
                            loadTypeLabels().getOrDefault(application.getApplicationType(), application.getApplicationType()),
                            approver.getRealName(),
                            StringUtils.hasText(comment) ? "，原因：" + comment.trim() : ""));
        } else if (processEnded) {
            application.setStatus(STATUS_APPROVED);
            application.setCompleteTime(LocalDateTime.now());
            attendanceApprovalSyncService.syncApprovedApplication(application);
            notificationService.sendToUser(application.getApplicantId(),
                    String.format("您的%s申请已通过", loadTypeLabels().getOrDefault(application.getApplicationType(), application.getApplicationType())));
        } else {
            // 一级审批通过，进入二级审批
            notificationService.sendToUser(application.getApplicantId(),
                    String.format("您的%s申请已通过一级审批，等待二级审批", loadTypeLabels().getOrDefault(application.getApplicationType(), application.getApplicationType())));
            notifyLevel2Approver(task.getProcessInstanceId());
        }
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        String typeLabel = loadTypeLabels().getOrDefault(application.getApplicationType(), application.getApplicationType());
        auditService.log(approver.getUserId(), approver.getUsername(),
                approved ? "APPROVE" : "REJECT", "application", application.getApplicationId(),
                String.format("%s了 %s 的%s申请%s",
                        approved ? "通过" : "驳回",
                        application.getApplicantName(), typeLabel,
                        StringUtils.hasText(comment) ? "（意见：" + comment.trim() + "）" : ""));

        return Result.success(approved ? "审批已通过" : "申请已驳回", toVO(application, true, null));
    }

    private void autoCancelExpired() {
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<AppApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("status", STATUS_PROCESSING)
                .lt("end_time", now)
                .apply("create_time < end_time");  // 只取消时段结束前提交的申请，补卡等事后提交不受影响
        java.util.List<AppApplication> expiredList = applicationMapper.selectList(wrapper);
        for (AppApplication app : expiredList) {
            if (app.getProcessInstanceId() == null) continue;
            try {
                runtimeService.deleteProcessInstance(app.getProcessInstanceId(), "申请已过期，系统自动取消");
            } catch (Exception ignored) {
            }
            app.setStatus(STATUS_CANCELED);
            app.setCompleteTime(now);
            app.setUpdateTime(now);
            applicationMapper.updateById(app);
        }
    }

    private void ensureCanHandle(AppApplication application, SysUser approver) {
        if (dataScopeService.hasFullDataAccess()) {
            return;
        }
        if (!dataScopeService.hasDepartmentDataAccess()
                || approver.getDeptId() == null
                || !isDeptAncestorOf(approver.getDeptId(), application.getApplicantDeptId())) {
            throw new BusinessException(403, "只能处理本部门及子部门员工的申请");
        }
    }

    /**
     * 判断 candidateAncestorId 是否是 candidateDescendantId 的祖先部门（或同一部门）
     */
    private boolean isDeptAncestorOf(Long ancestorId, Long descendantId) {
        if (ancestorId == null || descendantId == null) {
            return false;
        }
        if (ancestorId.equals(descendantId)) {
            return true;
        }
        Long currentId = descendantId;
        while (currentId != null && currentId > 0) {
            SysDepartment dept = departmentMapper.selectById(currentId);
            if (dept == null || dept.getParentId() == null || dept.getParentId() == 0) {
                return false;
            }
            if (dept.getParentId().equals(ancestorId)) {
                return true;
            }
            currentId = dept.getParentId();
        }
        return false;
    }

    private void validateApplicationScope(ApplicationCreateDTO dto, SysUser applicant) {
        if (!TYPE_MAKEUP.equals(dto.getApplicationType())) {
            if (dto.getAttendanceRecordId() != null) {
                throw new BusinessException(400, "只有补卡申请可以关联考勤记录");
            }
            // 检查同一时段是否已有审批中或已通过的同类申请
            int overlap = applicationMapper.countOverlappingApplications(
                    applicant.getUserId(), dto.getApplicationType(),
                    dto.getStartTime(), dto.getEndTime());
            if (overlap > 0) {
                String typeLabel = loadTypeLabels().getOrDefault(dto.getApplicationType(), dto.getApplicationType());
                throw new BusinessException(409, "该时段已有审批中或已通过的" + typeLabel + "申请，请勿重复提交");
            }
            return;
        }
        if (dto.getAttendanceRecordId() == null) {
            throw new BusinessException(400, "请选择需要补卡的考勤记录");
        }

        AttRecord record = recordMapper.selectById(dto.getAttendanceRecordId());
        if (record == null || !applicant.getUserId().equals(record.getUserId())) {
            throw new BusinessException(400, "补卡记录不存在或不属于当前用户");
        }
        if (!isCorrectableRecord(record)) {
            throw new BusinessException(400, "只能对缺勤、迟到、早退或缺卡记录发起补卡");
        }
        if (record.getAttendanceDate().isAfter(java.time.LocalDate.now())) {
            throw new BusinessException(400, "不能对未来考勤记录发起补卡");
        }
        if (!record.getAttendanceDate().equals(dto.getStartTime().toLocalDate())
                || !record.getAttendanceDate().equals(dto.getEndTime().toLocalDate())) {
            throw new BusinessException(400, "补卡时间必须在所选考勤记录当天");
        }
        if (applicationMapper.countActiveMakeupApplications(record.getRecordId()) > 0) {
            throw new BusinessException(409, "该考勤记录已有审批中或已通过的补卡申请");
        }
    }

    private boolean isCorrectableRecord(AttRecord record) {
        return "ABSENT".equals(record.getAttendanceStatus())
                || "LATE".equals(record.getAttendanceStatus())
                || "EARLY".equals(record.getAttendanceStatus())
                || "ABSENT".equals(record.getCheckInStatus())
                || "LATE".equals(record.getCheckInStatus())
                || "EARLY".equals(record.getCheckOutStatus())
                || record.getCheckInTime() == null
                || record.getCheckOutTime() == null;
    }

    private MakeupRecordOptionVO toMakeupRecordOption(AttRecord record) {
        MakeupRecordOptionVO vo = new MakeupRecordOptionVO();
        BeanUtils.copyProperties(record, vo);
        vo.setIssueLabel(makeupIssueLabel(record));

        AttRule rule = ruleMapper.selectById(record.getRuleId());
        LocalDateTime suggestedStart = record.getCheckInTime();
        LocalDateTime suggestedEnd = record.getCheckOutTime();
        if (rule != null) {
            if (suggestedStart == null || "LATE".equals(record.getCheckInStatus())) {
                suggestedStart = LocalDateTime.of(record.getAttendanceDate(), rule.getWorkStartTime());
            }
            if (suggestedEnd == null || "EARLY".equals(record.getCheckOutStatus())) {
                suggestedEnd = LocalDateTime.of(record.getAttendanceDate(), rule.getWorkEndTime());
            }
        }
        vo.setSuggestedStartTime(suggestedStart);
        vo.setSuggestedEndTime(suggestedEnd);
        return vo;
    }

    private String makeupIssueLabel(AttRecord record) {
        List<String> issues = new ArrayList<>();
        if ("ABSENT".equals(record.getAttendanceStatus())) {
            issues.add("缺勤");
        }
        if (record.getCheckInTime() == null) {
            issues.add("缺签到");
        } else if ("LATE".equals(record.getCheckInStatus())) {
            issues.add("迟到");
        }
        if (record.getCheckOutTime() == null) {
            issues.add("缺签退");
        } else if ("EARLY".equals(record.getCheckOutStatus())) {
            issues.add("早退");
        }
        return String.join("、", issues);
    }

    private SysUser requireCurrentUser() {
        SysUser user = dataScopeService.getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "登录状态已失效");
        }
        return user;
    }

    private SysUser requireApprover() {
        SysUser user = requireCurrentUser();
        if (!dataScopeService.hasFullDataAccess() && !dataScopeService.hasDepartmentDataAccess()) {
            throw new BusinessException(403, "当前用户没有审批权限");
        }
        if (dataScopeService.hasDepartmentDataAccess() && user.getDeptId() == null) {
            throw new BusinessException("主管尚未配置所属部门");
        }
        return user;
    }

    /**
     * 查找比申请人职位等级高指定级数的审批人，沿部门树向上遍历
     * @param applicant 申请人
     * @param levelsAbove 高出几级（1=直属上级，2=上两级）
     * @return 审批人用户名
     */
    private Long resolveUserId(String username) {
        if (!StringUtils.hasText(username)) return null;
        SysUser user = userMapper.findByUsername(username);
        return user == null ? null : user.getUserId();
    }

    private void notifyLevel2Approver(String processInstanceId) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .singleResult();
        if (task != null && StringUtils.hasText(task.getAssignee())) {
            Long level2UserId = resolveUserId(task.getAssignee());
            AppApplication app = findByProcessInstanceId(processInstanceId);
            if (level2UserId != null && app != null) {
                notificationService.sendToUser(level2UserId,
                        String.format("%s 的%s申请已通过一级审批，请审批",
                                app.getApplicantName(),
                                loadTypeLabels().getOrDefault(app.getApplicationType(), app.getApplicationType())));
            }
        }
    }

    private String findApproverByLevel(SysUser applicant, int levelsAbove) {
        if (applicant.getPositionId() == null) {
            throw new BusinessException(400, "您尚未分配职位，无法提交申请");
        }
        SysPosition position = positionMapper.selectById(applicant.getPositionId());
        if (position == null || position.getLevel() == null) {
            throw new BusinessException(400, "您的职位信息异常，请联系管理员");
        }
        int targetLevel = position.getLevel() + levelsAbove;
        String levelLabel = levelsAbove == 1 ? "直属上级" : "上" + levelsAbove + "级";

        Long deptId = applicant.getDeptId();
        while (deptId != null && deptId > 0) {
            SysUser approver = userMapper.findApproverByDeptAndMinLevel(deptId, targetLevel);
            if (approver != null) {
                return approver.getUsername();
            }
            SysDepartment dept = departmentMapper.selectById(deptId);
            deptId = (dept == null || dept.getParentId() == null || dept.getParentId() == 0) ? null : dept.getParentId();
        }
        throw new BusinessException(400, "未找到您的" + levelLabel + "审批人，请联系管理员配置职位");
    }

    private AppApplication requireApplication(Long applicationId) {
        AppApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }
        return application;
    }

    private AppApplication findByProcessInstanceId(String processInstanceId) {
        QueryWrapper<AppApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("process_instance_id", processInstanceId);
        return applicationMapper.selectOne(wrapper);
    }

    private List<ApplicationVO> toVOList(List<AppApplication> applications, Task activeTask) {
        Map<String, String> typeLabels = loadTypeLabels();
        return applications.stream()
                .map(item -> toVO(item, false, activeTask, typeLabels))
                .collect(Collectors.toList());
    }

    private ApplicationVO toVO(AppApplication application, boolean includeHistory, Task knownTask) {
        return toVO(application, includeHistory, knownTask, loadTypeLabels());
    }

    private ApplicationVO toVO(AppApplication application, boolean includeHistory, Task knownTask,
                               Map<String, String> typeLabels) {
        ApplicationVO vo = new ApplicationVO();
        BeanUtils.copyProperties(application, vo, "attachmentUrls");
        vo.setAttachmentUrls(readAttachments(application.getAttachmentUrls()));
        vo.setApplicationTypeLabel(typeLabels.getOrDefault(
                application.getApplicationType(), application.getApplicationType()));
        vo.setStatusLabel(statusLabel(application.getStatus()));
        SysUser current = dataScopeService.getCurrentUser();
        vo.setCanCancel(STATUS_PROCESSING.equals(application.getStatus())
                && current != null && current.getUserId().equals(application.getApplicantId()));

        Task activeTask = knownTask;
        if (activeTask == null && StringUtils.hasText(application.getProcessInstanceId())
                && STATUS_PROCESSING.equals(application.getStatus())) {
            activeTask = taskService.createTaskQuery()
                    .processInstanceId(application.getProcessInstanceId())
                    .active()
                    .singleResult();
        }
        if (activeTask != null) {
            vo.setTaskId(activeTask.getId());
            vo.setCurrentTaskName(activeTask.getName());
        }
        if (includeHistory) {
            vo.setApprovalHistory(loadHistory(application.getProcessInstanceId()));
        }
        return vo;
    }

    private List<ApprovalHistoryVO> loadHistory(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return Collections.emptyList();
        }
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeTaskLocalVariables()
                .orderByHistoricTaskInstanceStartTime().asc()
                .list();
        List<ApprovalHistoryVO> result = new ArrayList<>();
        for (HistoricTaskInstance task : tasks) {
            ApprovalHistoryVO history = new ApprovalHistoryVO();
            history.setTaskName(task.getName());
            history.setApproverUsername(task.getAssignee());
            history.setStartTime(toLocalDateTime(task.getStartTime()));
            history.setEndTime(toLocalDateTime(task.getEndTime()));
            Map<String, Object> variables = task.getTaskLocalVariables();
            history.setDecision(stringValue(variables.get("decision")));
            history.setComment(stringValue(variables.get("approvalComment")));
            history.setApproverName(stringValue(variables.get("approverName")));
            if (!StringUtils.hasText(history.getApproverName()) && StringUtils.hasText(task.getAssignee())) {
                SysUser user = userMapper.findByUsername(task.getAssignee());
                history.setApproverName(user == null ? task.getAssignee() : user.getRealName());
            }
            result.add(history);
        }
        return result;
    }

    private Map<String, String> loadTypeLabels() {
        Map<String, String> result = new HashMap<>();
        for (ApplicationTypeVO item : applicationMapper.selectEnabledApplicationTypes()) {
            result.put(item.getValue(), item.getLabel());
        }
        return result;
    }

    private String writeAttachments(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return "[]";
        }
        List<String> cleaned = urls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        for (String url : cleaned) {
            if (url.length() > 1000) {
                throw new BusinessException(400, "单个附件地址不能超过1000个字符");
            }
            String lower = url.toLowerCase();
            if (!(lower.startsWith("http://") || lower.startsWith("https://") || url.startsWith("/"))) {
                throw new BusinessException(400, "附件地址仅支持 http、https 或站内路径");
            }
        }
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (JsonProcessingException e) {
            throw new BusinessException("申请附件格式错误");
        }
    }

    private List<String> readAttachments(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException e) {
            return Collections.singletonList(value);
        }
    }

    private String statusLabel(String status) {
        if (STATUS_PROCESSING.equals(status)) {
            return "审批中";
        }
        if (STATUS_APPROVED.equals(status)) {
            return "已通过";
        }
        if (STATUS_REJECTED.equals(status)) {
            return "已驳回";
        }
        if (STATUS_CANCELED.equals(status)) {
            return "已取消";
        }
        return "草稿";
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
