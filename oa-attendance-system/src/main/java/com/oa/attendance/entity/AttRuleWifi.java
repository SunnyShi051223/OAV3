package com.oa.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("att_rule_wifi")
public class AttRuleWifi {

    @TableId(value = "rule_wifi_id", type = IdType.AUTO)
    private Long ruleWifiId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("wifi_ssid")
    private String wifiSsid;

    @TableField("wifi_bssid")
    private String wifiBssid;

    @TableField("enabled")
    private Integer enabled;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
