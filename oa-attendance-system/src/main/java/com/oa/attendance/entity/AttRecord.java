package com.oa.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("att_record")
public class AttRecord {

    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    @TableField("user_id")
    private Long userId;

    @TableField("dept_id")
    private Long deptId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("rule_name")
    private String ruleName;

    @TableField("attendance_date")
    private LocalDate attendanceDate;

    @TableField("check_in_time")
    private LocalDateTime checkInTime;

    @TableField("check_out_time")
    private LocalDateTime checkOutTime;

    @TableField("check_in_status")
    private String checkInStatus;

    @TableField("check_out_status")
    private String checkOutStatus;

    @TableField("attendance_status")
    private String attendanceStatus;

    @TableField("check_in_location")
    private String checkInLocation;

    @TableField("check_in_latitude")
    private BigDecimal checkInLatitude;

    @TableField("check_in_longitude")
    private BigDecimal checkInLongitude;

    @TableField("check_in_wifi_ssid")
    private String checkInWifiSsid;

    @TableField("check_in_wifi_bssid")
    private String checkInWifiBssid;

    @TableField("check_in_distance")
    private Integer checkInDistance;

    @TableField("check_in_valid")
    private Integer checkInValid;

    @TableField("check_in_fail_reason")
    private String checkInFailReason;

    @TableField("check_out_location")
    private String checkOutLocation;

    @TableField("check_out_latitude")
    private BigDecimal checkOutLatitude;

    @TableField("check_out_longitude")
    private BigDecimal checkOutLongitude;

    @TableField("check_out_wifi_ssid")
    private String checkOutWifiSsid;

    @TableField("check_out_wifi_bssid")
    private String checkOutWifiBssid;

    @TableField("check_out_distance")
    private Integer checkOutDistance;

    @TableField("check_out_valid")
    private Integer checkOutValid;

    @TableField("check_out_fail_reason")
    private String checkOutFailReason;

    @TableField("work_minutes")
    private Integer workMinutes;

    @TableField("overtime_minutes")
    private Integer overtimeMinutes;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String realName;

    @TableField(exist = false)
    private String deptName;
}
