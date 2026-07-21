package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.AttRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttRuleMapper extends BaseMapper<AttRule> {

    @Select("SELECT r.*, d.dept_name " +
            "FROM att_rule r " +
            "LEFT JOIN sys_department d ON r.dept_id = d.dept_id " +
            "WHERE r.deleted = 0 " +
            "AND (#{deptId} IS NULL OR r.dept_id = #{deptId}) " +
            "ORDER BY r.create_time DESC")
    List<AttRule> selectRuleList(@Param("deptId") Long deptId);

    @Select("SELECT * FROM att_rule " +
            "WHERE deleted = 0 AND enabled = 1 " +
            "AND (dept_id = #{deptId} OR dept_id IS NULL) " +
            "AND (effective_start_date IS NULL OR effective_start_date <= #{attendanceDate}) " +
            "AND (effective_end_date IS NULL OR effective_end_date >= #{attendanceDate}) " +
            "ORDER BY CASE WHEN dept_id = #{deptId} THEN 0 ELSE 1 END, create_time DESC " +
            "LIMIT 1")
    AttRule selectActiveRule(@Param("deptId") Long deptId, @Param("attendanceDate") LocalDate attendanceDate);

    @Select("SELECT r.*, d.dept_name " +
            "FROM att_rule r " +
            "LEFT JOIN sys_department d ON r.dept_id = d.dept_id " +
            "WHERE r.deleted = 0 AND r.enabled = 1 " +
            "AND (r.dept_id = #{deptId} OR r.dept_id IS NULL) " +
            "AND (r.effective_start_date IS NULL OR r.effective_start_date <= #{attendanceDate}) " +
            "AND (r.effective_end_date IS NULL OR r.effective_end_date >= #{attendanceDate}) " +
            "ORDER BY CASE WHEN r.dept_id = #{deptId} THEN 0 ELSE 1 END, r.work_start_time ASC, r.create_time DESC")
    List<AttRule> selectAvailableRules(@Param("deptId") Long deptId, @Param("attendanceDate") LocalDate attendanceDate);
}
