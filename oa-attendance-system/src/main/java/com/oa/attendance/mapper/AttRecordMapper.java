package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.AttRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttRecordMapper extends BaseMapper<AttRecord> {

    @Select("SELECT r.*, u.real_name, d.dept_name " +
            "FROM att_record r " +
            "LEFT JOIN sys_user u ON r.user_id = u.user_id " +
            "LEFT JOIN sys_department d ON r.dept_id = d.dept_id " +
            "WHERE r.user_id = #{userId} AND r.attendance_date BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY r.attendance_date ASC")
    List<AttRecord> selectUserCalendar(@Param("userId") Long userId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    @Select("SELECT r.*, u.real_name, d.dept_name " +
            "FROM att_record r " +
            "LEFT JOIN sys_user u ON r.user_id = u.user_id " +
            "LEFT JOIN sys_department d ON r.dept_id = d.dept_id " +
            "WHERE r.record_id = #{recordId}")
    AttRecord selectDetailById(@Param("recordId") Long recordId);

    @Select("SELECT r.*, u.real_name, d.dept_name " +
            "FROM att_record r " +
            "LEFT JOIN sys_user u ON r.user_id = u.user_id " +
            "LEFT JOIN sys_department d ON r.dept_id = d.dept_id " +
            "WHERE r.user_id = #{userId} AND r.attendance_date <= CURRENT_DATE " +
            "AND (r.attendance_status IN ('ABSENT', 'LATE', 'EARLY') " +
            "OR r.check_in_status IN ('ABSENT', 'LATE') OR r.check_out_status = 'EARLY' " +
            "OR r.check_in_time IS NULL OR r.check_out_time IS NULL) " +
            "AND NOT EXISTS (SELECT 1 FROM app_application a " +
            "WHERE a.attendance_record_id = r.record_id AND a.application_type = 'MAKEUP' " +
            "AND a.status IN ('PROCESSING', 'APPROVED')) " +
            "ORDER BY r.attendance_date DESC, r.record_id DESC")
    List<AttRecord> selectCorrectableRecords(@Param("userId") Long userId);
}
