package com.oa.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("att_rule")
public class AttRule {

    @TableId(value = "rule_id", type = IdType.AUTO)
    private Long ruleId;

    @TableField("rule_name")
    private String ruleName;

    @TableField("dept_id")
    private Long deptId;

    @TableField("effective_start_date")
    private LocalDate effectiveStartDate;

    @TableField("effective_end_date")
    private LocalDate effectiveEndDate;

    @TableField("work_start_time")
    private LocalTime workStartTime;

    @TableField("work_end_time")
    private LocalTime workEndTime;

    @TableField("check_in_start_time")
    private LocalTime checkInStartTime;

    @TableField("check_in_end_time")
    private LocalTime checkInEndTime;

    @TableField("check_out_start_time")
    private LocalTime checkOutStartTime;

    @TableField("check_out_end_time")
    private LocalTime checkOutEndTime;

    @TableField("late_threshold")
    private Integer lateThreshold;

    @TableField("early_threshold")
    private Integer earlyThreshold;

    @TableField("overtime_threshold")
    private Integer overtimeThreshold;

    @TableField("work_days")
    private String workDays;

    @TableField("require_wifi")
    private Integer requireWifi;

    @TableField("require_location")
    private Integer requireLocation;

    @TableField("enabled")
    private Integer enabled;

    @TableField("created_by")
    private Long createdBy;

    @TableField("deleted")
    private Integer deleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String deptName;
}
