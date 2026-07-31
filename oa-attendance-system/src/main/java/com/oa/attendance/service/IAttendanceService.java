package com.oa.attendance.service;

import com.oa.attendance.dto.AttendanceCheckDTO;
import com.oa.attendance.dto.AttendanceRuleDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.vo.AttendanceDetailVO;
import com.oa.attendance.vo.AttendanceMonthVO;
import com.oa.attendance.vo.AttendanceRecordVO;
import com.oa.attendance.vo.AttendanceRuleVO;
import com.oa.attendance.vo.AttendanceStatsVO;

import java.time.LocalDate;
import java.util.List;

public interface IAttendanceService {

    Result<?> createRule(AttendanceRuleDTO dto);

    Result<?> updateRule(AttendanceRuleDTO dto);

    Result<?> deleteRule(Long ruleId);

    Result<AttendanceRuleVO> getRule(Long ruleId);

    Result<List<AttendanceRuleVO>> listRules(Long deptId);

    Result<AttendanceRuleVO> getCurrentRule();

    Result<List<AttendanceRuleVO>> getAvailableRules();

    Result<AttendanceRecordVO> checkIn(AttendanceCheckDTO dto);

    Result<AttendanceRecordVO> checkOut(AttendanceCheckDTO dto);

    Result<AttendanceRecordVO> getToday();

    Result<List<AttendanceRecordVO>> getTodayRecords();

    Result<AttendanceMonthVO> getMyMonth(String month);

    Result<AttendanceStatsVO> getStats(LocalDate startDate, LocalDate endDate, Long deptId);

    Result<List<AttendanceDetailVO>> getStatsDetail(LocalDate startDate, LocalDate endDate, Long deptId);

    Result<?> modifyRecord(com.oa.attendance.dto.AttendanceModifyDTO dto);

    Result<com.oa.attendance.vo.AttendanceMonthVO> getUserMonth(Long userId, String month);

    Result<?> syncApplication(Long applicationId);
}
