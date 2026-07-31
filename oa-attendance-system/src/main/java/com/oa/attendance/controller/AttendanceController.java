package com.oa.attendance.controller;

import com.oa.attendance.dto.AttendanceCheckDTO;
import com.oa.attendance.dto.AttendanceModifyDTO;
import com.oa.attendance.dto.AttendanceRuleDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IAttendanceService;
import com.oa.attendance.vo.AttendanceDetailVO;
import com.oa.attendance.vo.AttendanceMonthVO;
import com.oa.attendance.vo.AttendanceRecordVO;
import com.oa.attendance.vo.AttendanceRuleVO;
import com.oa.attendance.vo.AttendanceStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@Validated
@CrossOrigin
public class AttendanceController {

    @Autowired
    private IAttendanceService attendanceService;

    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('attendance:rule:add')")
    public Result<?> createRule(@Valid @RequestBody AttendanceRuleDTO dto) {
        return attendanceService.createRule(dto);
    }

    @PutMapping("/rules")
    @PreAuthorize("hasAuthority('attendance:rule:update')")
    public Result<?> updateRule(@Valid @RequestBody AttendanceRuleDTO dto) {
        return attendanceService.updateRule(dto);
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasAuthority('attendance:rule:delete')")
    public Result<?> deleteRule(@PathVariable Long ruleId) {
        return attendanceService.deleteRule(ruleId);
    }

    @GetMapping("/rules/{ruleId}")
    @PreAuthorize("hasAuthority('attendance:rule:query')")
    public Result<AttendanceRuleVO> getRule(@PathVariable Long ruleId) {
        return attendanceService.getRule(ruleId);
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('attendance:rule:query')")
    public Result<List<AttendanceRuleVO>> listRules(@RequestParam(required = false) Long deptId) {
        return attendanceService.listRules(deptId);
    }

    @GetMapping("/current-rule")
    @PreAuthorize("hasAuthority('attendance:self')")
    public Result<AttendanceRuleVO> getCurrentRule() {
        return attendanceService.getCurrentRule();
    }

    @GetMapping("/available-rules")
    @PreAuthorize("hasAuthority('attendance:self')")
    public Result<List<AttendanceRuleVO>> getAvailableRules() {
        return attendanceService.getAvailableRules();
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('attendance:check')")
    public Result<AttendanceRecordVO> checkIn(@RequestBody AttendanceCheckDTO dto) {
        return attendanceService.checkIn(dto);
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAuthority('attendance:check')")
    public Result<AttendanceRecordVO> checkOut(@RequestBody AttendanceCheckDTO dto) {
        return attendanceService.checkOut(dto);
    }

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('attendance:self')")
    public Result<AttendanceRecordVO> getToday() {
        return attendanceService.getToday();
    }

    @GetMapping("/today-records")
    @PreAuthorize("hasAuthority('attendance:self')")
    public Result<List<AttendanceRecordVO>> getTodayRecords() {
        return attendanceService.getTodayRecords();
    }

    @GetMapping("/my-month")
    @PreAuthorize("hasAuthority('attendance:self')")
    public Result<AttendanceMonthVO> getMyMonth(@RequestParam(required = false) String month) {
        return attendanceService.getMyMonth(month);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('attendance:stats')")
    public Result<AttendanceStatsVO> getStats(@RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                              @RequestParam(required = false) Long deptId) {
        return attendanceService.getStats(startDate, endDate, deptId);
    }

    @GetMapping("/stats/detail")
    @PreAuthorize("hasAuthority('attendance:stats')")
    public Result<List<AttendanceDetailVO>> getStatsDetail(@RequestParam(required = false)
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                           @RequestParam(required = false)
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                           @RequestParam(required = false) Long deptId) {
        return attendanceService.getStatsDetail(startDate, endDate, deptId);
    }

    @PutMapping("/records/modify")
    @PreAuthorize("hasAuthority('attendance:modify')")
    public Result<?> modifyRecord(@Valid @RequestBody AttendanceModifyDTO dto) {
        return attendanceService.modifyRecord(dto);
    }

    @PostMapping("/sync-application/{applicationId}")
    @PreAuthorize("hasAuthority('attendance:modify')")
    public Result<?> syncApplication(@PathVariable Long applicationId) {
        return attendanceService.syncApplication(applicationId);
    }

    @GetMapping("/user-month")
    @PreAuthorize("hasAuthority('attendance:modify')")
    public Result<AttendanceMonthVO> getUserMonth(@RequestParam Long userId,
                                                   @RequestParam(required = false) String month) {
        return attendanceService.getUserMonth(userId, month);
    }
}
