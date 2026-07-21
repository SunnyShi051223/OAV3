package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MakeupRecordOptionVO {
    private Long recordId;
    private Long ruleId;
    private String ruleName;
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String checkInStatus;
    private String checkOutStatus;
    private String attendanceStatus;
    private String issueLabel;
    private LocalDateTime suggestedStartTime;
    private LocalDateTime suggestedEndTime;
}
