package com.oa.attendance.controller;

import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysAuditLog;
import com.oa.attendance.service.impl.ApprovalReminderScheduler;
import com.oa.attendance.service.impl.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin
public class AuditController {

    @Autowired
    private AuditService auditService;

    @Autowired
    private ApprovalReminderScheduler reminderScheduler;

    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('approval:all')")
    public Result<List<SysAuditLog>> recent(@RequestParam(defaultValue = "50") int limit) {
        return Result.success("查询成功", auditService.findRecent(limit));
    }

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAuthority('approval:all')")
    public Result<List<SysAuditLog>> byApplication(@PathVariable Long applicationId) {
        return Result.success("查询成功", auditService.findByTarget("application", applicationId));
    }

    @PostMapping("/trigger-email-reminder")
    @PreAuthorize("hasAuthority('approval:all')")
    public Result<Map<String, Object>> triggerEmailReminder(@RequestParam(defaultValue = "0") int hoursAgo,
                                                @RequestParam(defaultValue = "false") boolean force) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int count = reminderScheduler.remindNow(hoursAgo, force);
            result.put("sent", count);
            result.put("message", String.format("检查 %d 小时前，发送 %d 封", hoursAgo == 0 ? 3 : hoursAgo, count));
        } catch (Exception e) {
            result.put("sent", 0);
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return Result.success(result);
    }

    @GetMapping("/smtp-test")
    @PreAuthorize("hasAuthority('approval:all')")
    public Result<Map<String, Object>> testSmtp() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress("smtp.qq.com", 465), 10000);
            result.put("smtp_connect", "OK - smtp.qq.com:465 reachable");
            socket.close();
        } catch (Exception e) {
            result.put("smtp_connect", "FAILED: " + e.getMessage());
        }
        try {
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress("smtp.qq.com", 587), 10000);
            result.put("smtp_587", "OK - smtp.qq.com:587 reachable");
            socket.close();
        } catch (Exception e) {
            result.put("smtp_587", "FAILED: " + e.getMessage());
        }
        result.put("mail_config", "host=smtp.qq.com, port=465, ssl=true, user=3938706514@qq.com");
        return Result.success(result);
    }
}
