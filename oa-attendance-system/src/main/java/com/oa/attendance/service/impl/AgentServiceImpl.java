package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.attendance.dto.ApplicationCreateDTO;
import com.oa.attendance.dto.ApprovalDecisionDTO;
import com.oa.attendance.dto.AttendanceCheckDTO;
import com.oa.attendance.entity.AppApplication;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.AppApplicationMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.*;
import com.oa.attendance.vo.ApplicationVO;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentServiceImpl implements IAgentService {

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final int MAX_CONTEXT = 10;

    @Value("${agent.deepseek.key:sk-YOURKEY}")
    private String apiKey;

    private static final String SYSTEM_PROMPT =
        "你是OA办公系统的AI助手小汇。根据用户的自然语言输入，判断意图并返回JSON。\n\n" +
        "当前日期时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n\n" +
        "可用功能(intent):\n" +
        "- check_in: 用户要签到打卡。只需reply文本。\n" +
        "- check_out: 用户要签退打卡。只需reply文本。\n" +
        "- submit_leave: 用户要请假。params需含startTime(ISO格式)、endTime、reason\n" +
        "- submit_overtime: 用户要加班。params需含startTime、endTime、reason\n" +
        "- approve: 用户要审批通过。reply中告知结果。\n" +
        "- reject: 用户要审批驳回。reply中告知结果。\n" +
        "- query_attendance: 用户要查考勤。只需reply文本。\n" +
        "- query_applications: 用户要查自己的申请。只需reply文本。\n" +
        "- query_pending: 用户要查看待审批的申请（别人提交的需要自己审批的）。只需reply文本。\n" +
        "- query_profile: 用户要查个人信息。只需reply文本。\n" +
        "- send_message: 用户要给某人发消息。params需含target(收件人姓名或角色，如管理员/经理/组长)、content(消息内容)\n" +
        "- chat: 闲聊或其他无法分类的请求。只需reply文本。\n\n" +
        "规则：\n" +
        "1. 时间解析：今天/明天/后天/下周一等要转成具体日期；9点=09:00\n" +
        "2. 如果用户没给出完整时间信息，intent填对应功能但用reply引导用户补充\n" +
        "3. params中时间用ISO格式如2026-07-22T09:00:00\n" +
        "4. 只返回JSON，不要额外文本：{\"intent\":\"xxx\",\"reply\":\"回复内容\",\"params\":{}}\n";

    @Autowired private DataScopeService dataScopeService;
    @Autowired private IAttendanceService attendanceService;
    @Autowired private IApplicationService applicationService;
    @Autowired private NotificationService notificationService;
    @Autowired private SysUserMapper userMapper;
    @Autowired private AppApplicationMapper applicationMapper;
    @Autowired(required = false) private TaskService taskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 用户对话上下文缓存 */
    private final ConcurrentHashMap<Long, LinkedList<Map<String, String>>> contextStore = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> process(String message) {
        SysUser user = dataScopeService.getCurrentUser();
        if (user == null) return Result.error("请先登录");

        // 加载上下文
        List<Map<String, String>> history = contextStore.computeIfAbsent(user.getUserId(),
                k -> new LinkedList<>());

        // 0. Pre-check for send_message pattern before calling AI
        Map<String, Object> preCheck = preCheckSendMessage(message);
        if (preCheck != null) {
            String target = (String) preCheck.get("target");
            String content = (String) preCheck.get("content");
            String err = executeSendMessage(user,
                    Map.of("target", target, "content", content));
            String reply = err != null && err.startsWith("__OVERRIDE__")
                    ? err.substring(12) : "消息已发送";
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("reply", reply);
            result.put("intent", "send_message");
            history.add(Map.of("role", "user", "content", message));
            history.add(Map.of("role", "assistant", "content", reply));
            while (history.size() > MAX_CONTEXT * 2) { history.removeFirst(); history.removeFirst(); }
            return Result.success("ok", result);
        }

        // 1. Call DeepSeek with context
        Map<String, Object> llmResult = callDeepSeek(message, history);
        if (llmResult == null) {
            return processKeywordFallback(user, message);
        }

        String intent = (String) llmResult.getOrDefault("intent", "chat");
        String reply = (String) llmResult.getOrDefault("reply", "好的，我来处理。");
        Map<String, Object> params = (Map<String, Object>) llmResult.getOrDefault("params", new HashMap<>());

        // 2. Execute action and build human-readable error
        String actionError = null;
        try {
            switch (intent) {
                case "check_in":
                    actionError = executeCheckIn();
                    break;
                case "check_out":
                    actionError = executeCheckOut();
                    break;
                case "submit_leave":
                case "submit_overtime":
                    actionError = executeSubmit(intent, params, message);
                    break;
                case "approve":
                case "reject":
                    actionError = executeApprove(intent, user, message, reply);
                    if (actionError != null && actionError.startsWith("__OVERRIDE__")) {
                        reply = actionError.substring(12);
                        actionError = null;
                    }
                    break;
                case "query_attendance":
                    actionError = executeQueryAttendance();
                    if (actionError != null && !actionError.startsWith("__ERR__")) {
                        reply = actionError;
                        actionError = null;
                    }
                    break;
                case "query_applications":
                    actionError = executeQueryApplications();
                    if (actionError != null && !actionError.startsWith("__ERR__")) {
                        reply = actionError;
                        actionError = null;
                    }
                    break;
                case "query_pending":
                    actionError = executeQueryPending(user);
                    if (actionError != null && !actionError.startsWith("__ERR__")) {
                        reply = actionError;
                        actionError = null;
                    }
                    break;
                case "query_profile":
                    actionError = executeQueryProfile(user);
                    if (actionError != null && !actionError.startsWith("__ERR__")) {
                        reply = actionError;
                        actionError = null;
                    }
                    break;
                case "send_message":
                    actionError = executeSendMessage(user, params);
                    if (actionError != null && actionError.startsWith("__OVERRIDE__")) {
                        reply = actionError.substring(12);
                        actionError = null;
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            actionError = "操作异常：" + e.getMessage();
        }

        // 如果执行出错，用友好提示覆盖 reply
        if (actionError != null) {
            reply = actionError;
        }

        // 3. Save context
        history.add(Map.of("role", "user", "content", message));
        history.add(Map.of("role", "assistant", "content", reply));
        while (history.size() > MAX_CONTEXT * 2) {
            history.removeFirst();
            history.removeFirst();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply", reply != null ? reply : "已处理");
        result.put("intent", intent);
        return Result.success("ok", result);
    }

    // ===== Action Executors (return error message or null on success) =====

    private String executeCheckIn() {
        try {
            AttendanceCheckDTO dto = new AttendanceCheckDTO();
            dto.setLocationAddress("AI 语音打卡");
            Result<?> r = attendanceService.checkIn(dto);
            if (r.getCode() != 200) return buildCheckError("签到", r.getMsg());
            return null;
        } catch (Exception e) {
            return buildCheckError("签到", e.getMessage());
        }
    }

    private String executeCheckOut() {
        try {
            AttendanceCheckDTO dto = new AttendanceCheckDTO();
            dto.setLocationAddress("AI 语音打卡");
            Result<?> r = attendanceService.checkOut(dto);
            if (r.getCode() != 200) return buildCheckError("签退", r.getMsg());
            return null;
        } catch (Exception e) {
            return buildCheckError("签退", e.getMessage());
        }
    }

    private String buildCheckError(String action, String errMsg) {
        String lower = errMsg != null ? errMsg.toLowerCase() : "";
        if (lower.contains("定位") || lower.contains("位置") || lower.contains("location")) {
            return "无法" + action + "：当前考勤规则要求定位校验，AI 助手无法获取您的 GPS 坐标。"
                    + "\n\n请在打卡页面点击【获取当前位置】后手动打卡。";
        }
        if (lower.contains("wifi")) {
            return "无法" + action + "：当前考勤规则要求 WiFi 校验，AI 助手无法检测您的网络环境。"
                    + "\n\n请在打卡页面连接指定 WiFi 后手动打卡。";
        }
        if (lower.contains("不在") || lower.contains("范围") || lower.contains("允许")) {
            return "无法" + action + "：当前位置/WiFi 不在考勤规则允许范围内。"
                    + "\n\n请确认您在办公地点并已连接企业 WiFi。";
        }
        if (lower.contains("时间") || lower.contains("窗口") || lower.contains("未到") || lower.contains("超过")) {
            return "无法" + action + "：" + errMsg + "\n\n请在规定时间内打卡。";
        }
        if (lower.contains("请假") || lower.contains("leave")) {
            return "无法" + action + "：您当前处于已批准的请假时段，无需打卡。";
        }
        return "无法" + action + "：" + (errMsg != null ? errMsg : "未知错误")
                + "\n\n请前往打卡页面手动操作。";
    }

    private String executeSubmit(String intent, Map<String, Object> params, String message) {
        try {
            String type = "submit_leave".equals(intent) ? "LEAVE" : "OVERTIME";
            ApplicationCreateDTO dto = new ApplicationCreateDTO();
            dto.setApplicationType(type);
            dto.setStartTime(parseTime(params.get("startTime")));
            dto.setEndTime(parseTime(params.get("endTime")));
            dto.setReason(Objects.toString(params.get("reason"), message));
            dto.setRemark("AI 助手代提交");
            dto.setAttachmentUrls(Collections.emptyList());
            applicationService.submit(dto);
            return null;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            if (errMsg != null && errMsg.contains("职位")) {
                return "提交失败：您尚未分配职位，无法提交申请。请联系管理员完善您的职位信息。";
            }
            if (errMsg != null && errMsg.contains("审批人")) {
                return "提交失败：" + errMsg + "。请联系管理员完善组织架构。";
            }
            if (errMsg != null && errMsg.contains("重复")) {
                return "提交失败：" + errMsg;
            }
            return "提交失败：" + (errMsg != null ? errMsg : "未知错误") + "\n\n请前往审批页面手动提交。";
        }
    }

    private String executeApprove(String intent, SysUser user, String message, String reply) {
        try {
            List<AppApplication> pending = getPendingTasks(user);
            if (pending.isEmpty()) {
                return "__OVERRIDE__您当前没有待审批的申请。";
            }
            AppApplication target = pending.get(0);
            if (taskService != null) {
                Task task = taskService.createTaskQuery()
                        .processInstanceId(target.getProcessInstanceId()).active().singleResult();
                if (task != null) {
                    if ("approve".equals(intent)) {
                        applicationService.approve(task.getId(), null);
                        return "__OVERRIDE__已通过 " + target.getApplicantName() + " 的" + typeLabel(target.getApplicationType()) + "申请。";
                    } else {
                        ApprovalDecisionDTO dec = new ApprovalDecisionDTO();
                        String comment = message.length() > 200 ? message.substring(0, 200) : message;
                        dec.setComment(comment);
                        applicationService.reject(task.getId(), dec);
                        return "__OVERRIDE__已驳回 " + target.getApplicantName() + " 的" + typeLabel(target.getApplicationType()) + "申请。";
                    }
                }
            }
            return "__OVERRIDE__未找到可处理的审批任务。";
        } catch (Exception e) {
            String errMsg = e.getMessage();
            if (errMsg != null && (errMsg.contains("权限") || errMsg.contains("无权"))) {
                return "审批失败：您没有权限处理该申请。只有该申请人所在部门及上级部门的主管可以审批。";
            }
            if (errMsg != null && (errMsg.contains("已处理") || errMsg.contains("其他人"))) {
                return "该申请已被其他人处理，请刷新后查看。";
            }
            return "审批失败：" + (errMsg != null ? errMsg : "未知错误") + "\n\n请前往审批页面手动处理。";
        }
    }

    private String executeQueryAttendance() {
        try {
            Result<?> r = attendanceService.getMyMonth(null);
            Map<String, Object> data = objectMapper.convertValue(r.getData(), Map.class);
            return "本月考勤统计：\n• 工时 " + toHours(data.get("workMinutes")) +
                    "\n• 迟到 " + data.get("lateCount") + " 次" +
                    "\n• 缺勤 " + data.get("absentCount") + " 次" +
                    "\n• 加班 " + data.get("overtimeCount") + " 次" +
                    "\n• 请假 " + data.get("leaveCount") + " 次";
        } catch (Exception e) {
            return "__ERR__查询考勤失败：" + e.getMessage();
        }
    }

    private String executeQueryPending(SysUser user) {
        try {
            Result<List<ApplicationVO>> r = applicationService.listPendingTasks();
            List<ApplicationVO> apps = r.getData();
            if (apps == null || apps.isEmpty()) {
                return "您当前没有待审批的申请。";
            }
            StringBuilder sb = new StringBuilder("您有 " + apps.size() + " 条待审批申请：\n");
            for (int i = 0; i < Math.min(apps.size(), 5); i++) {
                ApplicationVO app = apps.get(i);
                sb.append("• ").append(app.getApplicantName())
                        .append(" · ").append(typeLabel(app.getApplicationType()))
                        .append(" · ").append(app.getStartTime() != null
                                ? app.getStartTime().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) : "?")
                        .append(" 至 ").append(app.getEndTime() != null
                                ? app.getEndTime().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) : "?")
                        .append("\n  原因：").append(app.getReason() != null && app.getReason().length() > 30
                                ? app.getReason().substring(0, 30) + "…" : app.getReason())
                        .append("\n");
            }
            sb.append("请前往审批中心处理。");
            return sb.toString();
        } catch (Exception e) {
            return "__ERR__查询待审批失败：" + e.getMessage();
        }
    }

    private String executeQueryApplications() {
        try {
            Result<List<ApplicationVO>> r = applicationService.listMine();
            List<ApplicationVO> apps = r.getData();
            if (apps == null || apps.isEmpty()) {
                return "您还没有提交过申请。在审批页面可以提交请假、加班、补卡申请。";
            }
            StringBuilder sb = new StringBuilder("您的申请记录：\n");
            for (int i = 0; i < Math.min(apps.size(), 5); i++) {
                ApplicationVO app = apps.get(i);
                sb.append("• [").append(statusLabel(app.getStatus())).append("] ")
                        .append(typeLabel(app.getApplicationType())).append(" ")
                        .append(app.getStartTime() != null ? app.getStartTime().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) : "?")
                        .append("\n");
            }
            sb.append("共 ").append(apps.size()).append(" 条，详情请查看审批页面。");
            return sb.toString();
        } catch (Exception e) {
            return "__ERR__查询申请失败：" + e.getMessage();
        }
    }

    private String executeQueryProfile(SysUser user) {
        try {
            SysUser detail = userMapper.getUserDetailById(user.getUserId());
            if (detail == null) detail = user;
            return "个人信息：\n• 姓名：" + detail.getRealName() +
                    "\n• 工号：" + detail.getEmployeeNo() +
                    "\n• 部门：" + (detail.getDeptName() != null ? detail.getDeptName() : "未分配") +
                    "\n• 职位：" + (detail.getPositionName() != null ? detail.getPositionName() : "未分配") +
                    "\n• 角色：" + (detail.getRoleName() != null ? detail.getRoleName() : "员工");
        } catch (Exception e) {
            return "__ERR__查询个人信息失败：" + e.getMessage();
        }
    }

    private String executeSendMessage(SysUser sender, Map<String, Object> params) {
        try {
            String target = Objects.toString(params.get("target"), "");
            String content = Objects.toString(params.get("content"), "");
            if (content.isEmpty()) return "__OVERRIDE__请告诉我您想发送的消息内容。";

            // 查找收件人
            SysUser receiver = findUserByNameOrRole(target);
            if (receiver == null)
                return "__OVERRIDE__未找到收件人「" + target + "」，请确认姓名或角色。可用的角色：管理员(admin)、经理(manager)、组长。";

            if (receiver.getUserId().equals(sender.getUserId()))
                return "__OVERRIDE__不能给自己发消息，请指定其他收件人。";

            notificationService.sendToUser(receiver.getUserId(),
                    sender.getRealName() + " 通过 AI 助手发来消息：\n" + content);

            return "__OVERRIDE__已成功将您的消息发送给 " + receiver.getRealName()
                    + "（" + (receiver.getDeptName() != null ? receiver.getDeptName() : "") + "）。";
        } catch (Exception e) {
            return "__OVERRIDE__发送失败：" + e.getMessage();
        }
    }

    private SysUser findUserByNameOrRole(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return null;
        String kw = keyword.trim();
        // 角色映射
        if (kw.contains("管理员") || kw.equalsIgnoreCase("admin")) {
            return userMapper.findByUsername("admin");
        }
        if (kw.contains("经理") || kw.contains("张经理") || kw.contains("张主管")) {
            return userMapper.findByUsername("manager");
        }
        // 按真实姓名精确匹配
        List<SysUser> all = userMapper.selectAllActiveWithDetails();
        for (SysUser u : all) {
            if (kw.equals(u.getRealName())) return u;
        }
        // 按姓名模糊匹配
        for (SysUser u : all) {
            if (u.getRealName() != null && u.getRealName().contains(kw)) return u;
        }
        // 按角色名匹配
        for (SysUser u : all) {
            if (u.getRoleName() != null && u.getRoleName().contains(kw)) return u;
        }
        return null;
    }

    /**
     * 前置检测：匹配 "给XX发消息：内容" 或 "发消息给XX：内容" 等模式
     */
    private Map<String, Object> preCheckSendMessage(String message) {
        if (message == null) return null;
        String colon = "[\\uff1a:]"; // 中文冒号或英文冒号
        // 模式1: 给XX发消息：内容
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(
                "给(.+?)发(?:消息|信息|通知)" + colon + "\\s*(.+)");
        java.util.regex.Matcher m1 = p1.matcher(message);
        if (m1.find()) {
            return Map.of("target", m1.group(1).trim(), "content", m1.group(2).trim());
        }
        // 模式2: 发消息给XX：内容
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(
                "发(?:消息|信息|通知)给(.+?)" + colon + "\\s*(.+)");
        java.util.regex.Matcher m2 = p2.matcher(message);
        if (m2.find()) {
            return Map.of("target", m2.group(1).trim(), "content", m2.group(2).trim());
        }
        // 模式3: 告诉XX：内容 / 通知XX：内容 / 转达XX：内容
        java.util.regex.Pattern p3 = java.util.regex.Pattern.compile(
                "(?:告诉|通知|转达)(.+?)" + colon + "\\s*(.+)");
        java.util.regex.Matcher m3 = p3.matcher(message);
        if (m3.find()) {
            return Map.of("target", m3.group(1).trim(), "content", m3.group(2).trim());
        }
        return null;
    }

    // ===== DeepSeek API Call with context =====
    @SuppressWarnings("unchecked")
    private Map<String, Object> callDeepSeek(String userMessage, List<Map<String, String>> history) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "deepseek-chat");
            body.put("temperature", 0.1);
            body.put("max_tokens", 800);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sysMsg = new LinkedHashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", SYSTEM_PROMPT);
            messages.add(sysMsg);

            // 添加上下文历史（最近几轮）
            int contextStart = Math.max(0, history.size() - MAX_CONTEXT * 2);
            for (int i = contextStart; i < history.size(); i++) {
                messages.add(new LinkedHashMap<>(history.get(i)));
            }

            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            body.put("messages", messages);
            body.put("response_format", Map.of("type", "json_object"));

            String requestJson = objectMapper.writeValueAsString(body);
            HttpURLConnection conn = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestJson.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                return null;
            }

            String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> respMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
            if (choices == null || choices.isEmpty()) return null;

            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) msg.get("content");
            if (content == null) return null;

            content = content.trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("```\\w*\\n?", "").replace("```", "").trim();
            }
            return objectMapper.readValue(content, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    // ===== Keyword Fallback =====
    private Result<Map<String, Object>> processKeywordFallback(SysUser user, String msg) {
        String lower = msg.toLowerCase();
        Map<String, Object> result = new LinkedHashMap<>();

        if (matches(lower, "签到", "上班", "check in")) {
            String err = executeCheckIn();
            result.put("reply", err != null ? err : "已为您签到!");
            result.put("intent", "check_in");
        } else if (matches(lower, "签退", "下班", "check out")) {
            String err = executeCheckOut();
            result.put("reply", err != null ? err : "已为您签退!");
            result.put("intent", "check_out");
        } else if (matches(lower, "考勤", "出勤", "统计")) {
            String r = executeQueryAttendance();
            result.put("reply", r != null ? r : "查询失败");
            result.put("intent", "query_attendance");
        } else if (matches(lower, "申请", "审批")) {
            String r = executeQueryApplications();
            result.put("reply", r != null ? r : "查询失败");
            result.put("intent", "query_applications");
        } else if (matches(lower, "待处理", "待审批", "pending", "需要我审", "我审批", "处理审批")) {
            String r = executeQueryPending(user);
            result.put("reply", r != null ? r : "查询失败");
            result.put("intent", "query_pending");
        } else if (matches(lower, "信息", "我是谁")) {
            String r = executeQueryProfile(user);
            result.put("reply", r != null ? r : "查询失败");
            result.put("intent", "query_profile");
        } else if (matches(lower, "发消息", "发送", "告诉", "转达", "通知")) {
            Map<String, Object> p = new HashMap<>();
            // 简单提取：发给XX + 内容
            p.put("target", msg);
            p.put("content", msg);
            String r = executeSendMessage(user, p);
            result.put("reply", r != null && r.startsWith("__OVERRIDE__") ? r.substring(12) : "请使用完整句子，如：给管理员发消息：你好！");
            result.put("intent", "send_message");
        } else {
            result.put("reply", "AI 服务暂不可用。您可以尝试：\n• 签到 / 签退\n• 考勤查询\n• 请假申请\n• 审批通过 / 驳回\n\n或者前往对应页面手动操作。");
            result.put("intent", "chat");
        }
        return Result.success("ok", result);
    }

    // ===== Helpers =====
    private boolean matches(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    private LocalDateTime parseTime(Object value) {
        if (value == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(value.toString().replace(" ", "T"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String toHours(Object minutes) {
        if (minutes == null) return "0h";
        return String.format("%.1fh", ((Number) minutes).doubleValue() / 60.0);
    }

    private String typeLabel(String type) {
        switch (type == null ? "" : type) {
            case "LEAVE": return "请假";
            case "OVERTIME": return "加班";
            case "MAKEUP": return "补卡";
            default: return type;
        }
    }

    private String statusLabel(String status) {
        switch (status == null ? "" : status) {
            case "PROCESSING": return "审批中";
            case "APPROVED": return "已通过";
            case "REJECTED": return "已驳回";
            case "CANCELED": return "已取消";
            default: return status;
        }
    }

    private List<AppApplication> getPendingTasks(SysUser user) {
        if (taskService == null) return Collections.emptyList();
        String group = dataScopeService.hasFullDataAccess() ? "approval_admin" : "approval_dept_" + user.getDeptId();
        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateGroup(group).active().orderByTaskCreateTime().desc().list();
        List<AppApplication> apps = new ArrayList<>();
        for (Task task : tasks) {
            QueryWrapper<AppApplication> w = new QueryWrapper<>();
            w.eq("process_instance_id", task.getProcessInstanceId());
            AppApplication app = applicationMapper.selectOne(w);
            if (app != null) apps.add(app);
        }
        return apps;
    }
}
