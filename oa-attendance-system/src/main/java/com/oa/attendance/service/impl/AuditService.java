package com.oa.attendance.service.impl;

import com.oa.attendance.entity.SysAuditLog;
import com.oa.attendance.mapper.SysAuditLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    @Autowired
    private SysAuditLogMapper auditLogMapper;

    public void log(Long userId, String username, String action, String targetType, Long targetId, String detail) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    public List<SysAuditLog> findByTarget(String targetType, Long targetId) {
        return auditLogMapper.findByTarget(targetType, targetId);
    }

    public List<SysAuditLog> findRecent(int limit) {
        return auditLogMapper.findRecent(limit);
    }
}
