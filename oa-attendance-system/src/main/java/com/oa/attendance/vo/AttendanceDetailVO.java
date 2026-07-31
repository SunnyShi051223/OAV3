package com.oa.attendance.vo;

import lombok.Data;

@Data
public class AttendanceDetailVO {
    private Long userId;
    private String employeeNo;
    private String realName;
    private String deptName;
    private Integer workDays;
    private Integer normalDays;
    private Integer lateCount;
    private Integer absentCount;
    private Integer leaveCount;
    private Integer overtimeCount;
    private Double workMinutes;
    private Double overtimeMinutes;
    private String attendanceRate;
}
