package com.oa.attendance.vo;

import lombok.Data;

import java.util.List;

@Data
public class AttendanceMonthVO {
    private String month;
    private Integer workMinutes;
    private Integer lateCount;
    private Integer absentCount;
    private Integer overtimeCount;
    private Integer overtimeMinutes;
    private Integer leaveCount;
    private Integer leaveMinutes;
    private List<AttendanceRecordVO> calendar;
}
