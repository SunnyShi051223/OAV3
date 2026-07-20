package com.oa.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("att_daily_summary")
public class AttDailySummary {

    @TableId(value = "summary_id", type = IdType.AUTO)
    private Long summaryId;

    @TableField("user_id")
    private Long userId;

    @TableField("dept_id")
    private Long deptId;

    @TableField("attendance_date")
    private LocalDate attendanceDate;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("record_id")
    private Long recordId;

    @TableField("attendance_status")
    private String attendanceStatus;

    @TableField("work_minutes")
    private Integer workMinutes;

    @TableField("late_minutes")
    private Integer lateMinutes;

    @TableField("early_minutes")
    private Integer earlyMinutes;

    @TableField("overtime_minutes")
    private Integer overtimeMinutes;

    @TableField("leave_minutes")
    private Integer leaveMinutes;

    @TableField("is_late")
    private Integer isLate;

    @TableField("is_absent")
    private Integer isAbsent;

    @TableField("is_overtime")
    private Integer isOvertime;

    @TableField("is_leave")
    private Integer isLeave;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
