package com.oa.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("att_rule_location")
public class AttRuleLocation {

    @TableId(value = "rule_location_id", type = IdType.AUTO)
    private Long ruleLocationId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("location_name")
    private String locationName;

    @TableField("latitude")
    private BigDecimal latitude;

    @TableField("longitude")
    private BigDecimal longitude;

    @TableField("radius")
    private Integer radius;

    @TableField("enabled")
    private Integer enabled;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
