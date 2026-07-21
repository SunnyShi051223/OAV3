package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceStatsVO {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long deptId;
    private String deptName;
    private Integer expectedCount;
    private Integer normalCount;
    private Integer lateCount;
    private Integer absentCount;
    private Integer leaveCount;
    private Integer overtimeCount;
    private String attendanceRate;
    private String lateRate;
    private String absentRate;
    private String leaveRate;
}
