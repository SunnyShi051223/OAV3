package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.attendance.dto.ApplicationCreateDTO;
import com.oa.attendance.dto.ApprovalDecisionDTO;
import com.oa.attendance.entity.AppApplication;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.exception.BusinessException;
import com.oa.attendance.mapper.AppApplicationMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.service.IAttendanceApprovalSyncService;
import com.oa.attendance.service.IApplicationService;
import com.oa.attendance.vo.ApplicationTypeVO;
import com.oa.attendance.vo.ApplicationVO;
import com.oa.attendance.vo.ApprovalHistoryVO;
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
    private static final String ADMIN_GROUP = "approval_admin";
    private static final String DEPT_GROUP_PREFIX = "approval_dept_";

    @Autowired
    private AppApplicationMapper applicationMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private IAttendanceApprovalSyncService attendanceApprovalSyncService;

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
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException(400, "结束时间不能早于开始时间");
        }

        Map<String, String> typeLabels = loadTypeLabels();
        if (!typeLabels.containsKey(dto.getApplicationType())) {
            throw new BusinessException(400, "申请类型不存在或已停用");
        }

        LocalDateTime now = LocalDateTime.now();
        AppApplication application = new AppApplication();
        BeanUtils.copyProperties(dto, application, "attachmentUrls");
        application.setApplicantId(applicant.getUserId());
        application.setApplicantName(applicant.getRealName());
        application.setApplicantDeptId(applicant.getDeptId());
        application.setApplicantDeptName(applicant.getDeptName());
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
        variables.put("candidateGroups", ADMIN_GROUP + "," + DEPT_GROUP_PREFIX + applicant.getDeptId());

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

        return Result.success("申请已提交", toVO(application, true, null));
    }

    @Override
    public Result<List<ApplicationVO>> listMine() {
        SysUser current = requireCurrentUser();
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
    public Result<List<ApplicationVO>> listPendingTasks() {
        SysUser current = requireApprover();
        String candidateGroup = dataScopeService.hasFullDataAccess()
                ? ADMIN_GROUP : DEPT_GROUP_PREFIX + current.getDeptId();
        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateGroup(candidateGroup)
                .active()
                .orderByTaskCreateTime().desc()
                .list();

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
            throw new BusinessException(409, "该任务已由其他审批人领取");
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

        application.setStatus(decision);
        application.setCompleteTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);
        if (approved) {
            attendanceApprovalSyncService.syncApprovedApplication(application);
        }
        return Result.success(approved ? "审批已通过" : "申请已驳回", toVO(application, true, null));
    }

    private void ensureCanHandle(AppApplication application, SysUser approver) {
        if (dataScopeService.hasFullDataAccess()) {
            return;
        }
        if (!dataScopeService.hasDepartmentDataAccess()
                || approver.getDeptId() == null
                || !approver.getDeptId().equals(application.getApplicantDeptId())) {
            throw new BusinessException(403, "只能处理本部门员工的申请");
        }
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
