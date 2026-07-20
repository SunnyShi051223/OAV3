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
@TableName("att_check_log")
public class AttCheckLog {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    @TableField("user_id")
    private Long userId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("record_id")
    private Long recordId;

    @TableField("attendance_date")
    private LocalDate attendanceDate;

    @TableField("check_type")
    private String checkType;

    @TableField("check_time")
    private LocalDateTime checkTime;

    @TableField("wifi_ssid")
    private String wifiSsid;

    @TableField("wifi_bssid")
    private String wifiBssid;

    @TableField("latitude")
    private BigDecimal latitude;

    @TableField("longitude")
    private BigDecimal longitude;

    @TableField("location_address")
    private String locationAddress;

    @TableField("distance")
    private Integer distance;

    @TableField("wifi_valid")
    private Integer wifiValid;

    @TableField("location_valid")
    private Integer locationValid;

    @TableField("success")
    private Integer success;

    @TableField("fail_reason")
    private String failReason;

    @TableField("client_info")
    private String clientInfo;

    @TableField("create_time")
    private LocalDateTime createTime;
}
