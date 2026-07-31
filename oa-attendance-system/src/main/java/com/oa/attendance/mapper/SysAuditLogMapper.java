package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {

    @Select("SELECT * FROM sys_audit_log WHERE target_type = #{targetType} AND target_id = #{targetId} ORDER BY create_time DESC")
    List<SysAuditLog> findByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("SELECT * FROM sys_audit_log ORDER BY create_time DESC LIMIT #{limit}")
    List<SysAuditLog> findRecent(@Param("limit") int limit);
}
