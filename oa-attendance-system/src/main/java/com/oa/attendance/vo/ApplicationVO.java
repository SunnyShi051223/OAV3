package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApplicationVO {
    private Long applicationId;
    private Long applicantId;
    private String applicantName;
    private Long applicantDeptId;
    private String applicantDeptName;
    private String applicationType;
    private String applicationTypeLabel;
    private Long attendanceRecordId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private List<String> attachmentUrls;
    private String remark;
    private String status;
    private String statusLabel;
    private String processInstanceId;
    private String taskId;
    private String currentTaskName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime completeTime;
    private Boolean canCancel;
    private List<ApprovalHistoryVO> approvalHistory;
}
