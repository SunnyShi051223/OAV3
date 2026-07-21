package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalHistoryVO {
    private String taskName;
    private String approverUsername;
    private String approverName;
    private String decision;
    private String comment;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
