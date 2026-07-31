package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.AppApplication;
import com.oa.attendance.vo.ApplicationTypeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AppApplicationMapper extends BaseMapper<AppApplication> {

    @Select("SELECT dict_value AS value, dict_label AS label " +
            "FROM sys_dict_data WHERE dict_code = 'application_type' AND status = 1 ORDER BY sort_order")
    List<ApplicationTypeVO> selectEnabledApplicationTypes();

    @Select("SELECT COUNT(*) FROM app_application " +
            "WHERE applicant_id = #{userId} AND application_type = 'LEAVE' " +
            "AND status = 'APPROVED' AND start_time <= #{checkTime} AND end_time > #{checkTime}")
    int countApprovedLeaveAt(@Param("userId") Long userId,
                             @Param("checkTime") LocalDateTime checkTime);

    @Select("SELECT COUNT(*) FROM app_application " +
            "WHERE attendance_record_id = #{recordId} AND application_type = 'MAKEUP' " +
            "AND status IN ('PROCESSING', 'APPROVED')")
    int countActiveMakeupApplications(@Param("recordId") Long recordId);

    @Select("SELECT COUNT(*) FROM app_application " +
            "WHERE applicant_id = #{userId} AND application_type = #{type} " +
            "AND status IN ('PROCESSING', 'APPROVED') " +
            "AND start_time < #{endTime} AND end_time > #{startTime}")
    int countOverlappingApplications(@Param("userId") Long userId,
                                     @Param("type") String type,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);
}
