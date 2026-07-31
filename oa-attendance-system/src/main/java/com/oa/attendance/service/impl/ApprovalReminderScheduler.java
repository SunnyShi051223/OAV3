package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.entity.AppApplication;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.AppApplicationMapper;
import com.oa.attendance.mapper.SysUserMapper;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 审批超时邮件提醒 —— 提交超过 3 小时未审批时，向当前审批人发送邮件
 */
@Component
public class ApprovalReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ApprovalReminderScheduler.class);
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private AppApplicationMapper applicationMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private TaskService taskService;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    /**
     * 每 15 分钟检查一次超时未审批的申请
     */
    @Scheduled(fixedDelay = 900_000, initialDelay = 120_000)
    public void remindOverdueApprovals() {
        doRemind(3);
    }

    /**
     * 手动触发（用于测试），hoursAgo: 超过多少小时未审批就发提醒，force: 强制重发
     */
    public int remindNow(int hoursAgo, boolean force) {
        if (mailSender == null) {
            log.warn("MailSender not configured");
            return 0;
        }
        return doRemind(hoursAgo, force);
    }

    /**
     * 紧急提醒：请假开始时间已到或即将开始，立即发送邮件
     */
    public boolean sendUrgentReminder(AppApplication application, String approverUsername) {
        if (mailSender == null) return false;
        if (application == null || !StringUtils.hasText(approverUsername)) return false;

        SysUser approver = userMapper.findByUsername(approverUsername);
        if (approver == null || !StringUtils.hasText(approver.getEmail())) return false;

        return sendUrgentEmail(approver, application);
    }

    private boolean sendUrgentEmail(SysUser approver, AppApplication application) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("3938706514@qq.com");
            helper.setTo(approver.getEmail());
            helper.setSubject("【OA 紧急审批】有一条请假申请需要立即处理");

            String typeLabel = resolveTypeLabel(application.getApplicationType());
            String applicantInfo = application.getApplicantName()
                    + (StringUtils.hasText(application.getApplicantDeptName())
                    ? "（" + application.getApplicantDeptName() + "）" : "");

            String html = buildUrgentEmailHtml(approver.getRealName(), applicantInfo, typeLabel, application);
            helper.setText(html, true);
            mailSender.send(message);

            log.info("Urgent reminder email sent to {} for application {}",
                    approver.getEmail(), application.getApplicationId());
            return true;
        } catch (Exception e) {
            log.error("Send urgent email to {} failed: {}", approver.getEmail(), e.getMessage());
            return false;
        }
    }

    private String buildUrgentEmailHtml(String approverName, String applicantInfo, String typeLabel, AppApplication app) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>"
                + "body{font-family:'Microsoft YaHei',sans-serif;background:#fff5f5;padding:30px;color:#333}"
                + ".card{max-width:560px;margin:0 auto;background:#fff;border-radius:12px;padding:32px;box-shadow:0 2px 12px rgba(0,0,0,.06);border-left:4px solid #f54a6e}"
                + "h2{color:#f54a6e;margin:0 0 8px}.urgent{color:#f54a6e;font-weight:700;margin:0 0 24px;font-size:14px}"
                + "table{width:100%;border-collapse:collapse;margin:16px 0}"
                + "td{padding:10px 12px;border-bottom:1px solid #f0f1f5;font-size:14px}"
                + "td:first-child{color:#8f959e;width:80px}"
                + ".tag{display:inline-block;padding:3px 10px;border-radius:4px;font-size:12px;color:#fff;background:#f54a6e}"
                + ".footer{margin-top:24px;color:#b1b5bb;font-size:12px;text-align:center}"
                + "</style></head><body><div class=\"card\">"
                + "<h2>OA 紧急审批提醒</h2>"
                + "<p class=\"urgent\">" + approverName + "，您好！以下申请即将开始或已经开始，请尽快处理。</p>"
                + "<table>"
                + "<tr><td>申请人</td><td>" + escapeHtml(applicantInfo) + "</td></tr>"
                + "<tr><td>申请类型</td><td>" + escapeHtml(typeLabel) + "</td></tr>"
                + "<tr><td>时间范围</td><td>" + formatTime(app.getStartTime()) + " 至 " + formatTime(app.getEndTime()) + "</td></tr>"
                + "<tr><td>申请原因</td><td>" + escapeHtml(app.getReason()) + "</td></tr>"
                + "<tr><td>提交时间</td><td>" + formatTime(app.getCreateTime()) + "</td></tr>"
                + "</table>"
                + "<div class=\"footer\">此邮件由 OA 系统自动发送，请勿回复。</div>"
                + "</div></body></html>";
    }

    private int doRemind(int hoursAgo) {
        return doRemind(hoursAgo, false);
    }

    private int doRemind(int hoursAgo, boolean force) {
        if (mailSender == null) {
            log.warn("MailSender not configured, skip email reminder");
            return 0;
        }

        List<AppApplication> overdueApps;
        try {
            overdueApps = applicationMapper.selectList(
                    new QueryWrapper<AppApplication>()
                            .eq("status", "PROCESSING")
                            .lt("create_time", LocalDateTime.now().minusHours(hoursAgo))
                            .isNotNull("process_instance_id"));
        } catch (Exception e) {
            log.error("Query overdue applications failed", e);
            return 0;
        }

        int count = 0;
        for (AppApplication app : overdueApps) {
            try {
                if (remindForApplication(app, force)) count++;
            } catch (Exception e) {
                log.error("Failed to send reminder for application {}", app.getApplicationId(), e);
            }
        }
        return count;
    }

    private boolean remindForApplication(AppApplication application, boolean force) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(application.getProcessInstanceId())
                .active()
                .singleResult();
        if (task == null) return false;

        if (!force) {
            Object reminded = taskService.getVariable(task.getId(), "emailReminded");
            if (Boolean.TRUE.equals(reminded)) return false;
        }

        String assignee = task.getAssignee();
        if (!StringUtils.hasText(assignee)) return false;

        SysUser approver = userMapper.findByUsername(assignee);
        if (approver == null || !StringUtils.hasText(approver.getEmail())) return false;

        boolean sent = sendReminderEmail(approver, application);
        if (sent) {
            taskService.setVariable(task.getId(), "emailReminded", true);
        }
        return sent;
    }

    private boolean sendReminderEmail(SysUser approver, AppApplication application) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("3938706514@qq.com");
            helper.setTo(approver.getEmail());
            helper.setSubject("【OA 审批提醒】您有一条待审批申请超过 3 小时");

            String typeLabel = resolveTypeLabel(application.getApplicationType());
            String applicantInfo = application.getApplicantName()
                    + (StringUtils.hasText(application.getApplicantDeptName())
                    ? "（" + application.getApplicantDeptName() + "）" : "");

            String html = buildEmailHtml(approver.getRealName(), applicantInfo, typeLabel,
                    application, taskService.createTaskQuery()
                            .processInstanceId(application.getProcessInstanceId())
                            .active().singleResult());

            helper.setText(html, true);
            mailSender.send(message);

            log.info("Reminder email sent to {} for application {}",
                    approver.getEmail(), application.getApplicationId());
            return true;
        } catch (Exception e) {
            log.error("Send email to {} failed: {}", approver.getEmail(), e.getMessage());
            return false;
        }
    }

    private String buildEmailHtml(String approverName, String applicantInfo, String typeLabel,
                                  AppApplication app, Task task) {
        String taskName = task != null ? task.getName() : "待审批";
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>"
                + "body{font-family:'Microsoft YaHei',sans-serif;background:#f5f6fa;padding:30px;color:#333}"
                + ".card{max-width:560px;margin:0 auto;background:#fff;border-radius:12px;padding:32px;box-shadow:0 2px 12px rgba(0,0,0,.06)}"
                + "h2{color:#3370ff;margin:0 0 8px}.sub{color:#8f959e;margin:0 0 24px;font-size:14px}"
                + "table{width:100%;border-collapse:collapse;margin:16px 0}"
                + "td{padding:10px 12px;border-bottom:1px solid #f0f1f5;font-size:14px}"
                + "td:first-child{color:#8f959e;width:80px}"
                + ".tag{display:inline-block;padding:3px 10px;border-radius:4px;font-size:12px;color:#fff;background:#f54a6e}"
                + ".footer{margin-top:24px;color:#b1b5bb;font-size:12px;text-align:center}"
                + "</style></head><body><div class=\"card\">"
                + "<h2>OA 审批提醒</h2>"
                + "<p class=\"sub\">" + approverName + "，您好！以下申请已提交超过 3 小时，请尽快处理。</p>"
                + "<table>"
                + "<tr><td>申请人</td><td>" + escapeHtml(applicantInfo) + "</td></tr>"
                + "<tr><td>申请类型</td><td>" + escapeHtml(typeLabel) + "</td></tr>"
                + "<tr><td>时间范围</td><td>" + formatTime(app.getStartTime()) + " 至 " + formatTime(app.getEndTime()) + "</td></tr>"
                + "<tr><td>申请原因</td><td>" + escapeHtml(app.getReason()) + "</td></tr>"
                + "<tr><td>当前节点</td><td><span class=\"tag\">" + escapeHtml(taskName) + "</span></td></tr>"
                + "<tr><td>提交时间</td><td>" + formatTime(app.getCreateTime()) + "</td></tr>"
                + "</table>"
                + "<div class=\"footer\">此邮件由 OA 系统自动发送，请勿回复。</div>"
                + "</div></body></html>";
    }

    private String resolveTypeLabel(String type) {
        try {
            return applicationMapper.selectEnabledApplicationTypes().stream()
                    .filter(t -> t.getValue().equals(type))
                    .findFirst()
                    .map(t -> t.getLabel())
                    .orElse(type);
        } catch (Exception e) {
            return type;
        }
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "—" : time.format(DATETIME_FMT);
    }

    private String escapeHtml(String text) {
        if (text == null) return "—";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
