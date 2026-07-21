package com.oa.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_application")
public class AppApplication {

    @TableId(value = "application_id", type = IdType.AUTO)
    private Long applicationId;

    @TableField("applicant_id")
    private Long applicantId;

    @TableField("applicant_name")
    private String applicantName;

    @TableField("applicant_dept_id")
    private Long applicantDeptId;

    @TableField("applicant_dept_name")
    private String applicantDeptName;

    @TableField("application_type")
    private String applicationType;

    @TableField("attendance_record_id")
    private Long attendanceRecordId;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("reason")
    private String reason;

    @TableField("attachment_urls")
    private String attachmentUrls;

    @TableField("remark")
    private String remark;

    @TableField("status")
    private String status;

    @TableField("process_instance_id")
    private String processInstanceId;

    @TableField("complete_time")
    private LocalDateTime completeTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
