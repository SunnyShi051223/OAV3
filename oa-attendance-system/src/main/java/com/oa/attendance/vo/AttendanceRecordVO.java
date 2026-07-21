package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AttendanceRecordVO {
    private Long recordId;
    private Long userId;
    private String realName;
    private Long deptId;
    private String deptName;
    private Long ruleId;
    private String ruleName;
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String checkInStatus;
    private String checkOutStatus;
    private String attendanceStatus;
    private Integer checkInValid;
    private Integer checkOutValid;
    private Integer workMinutes;
    private Integer overtimeMinutes;
}
