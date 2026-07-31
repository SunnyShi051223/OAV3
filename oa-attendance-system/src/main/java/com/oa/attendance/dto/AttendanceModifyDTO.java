package com.oa.attendance.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendanceModifyDTO {

    private Long recordId;
    private String attendanceStatus;  // NORMAL, LATE, EARLY, ABSENT, LEAVE, OVERTIME
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Integer workMinutes;
    private Integer overtimeMinutes;
    private String remark;            // 修改原因
}
