package com.oa.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 权限实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_permission")
public class SysPermission {

    @TableId(value = "permission_id", type = IdType.AUTO)
    private Long permissionId;           // 权限ID

    @TableField("permission_name")
    private String permissionName;       // 权限名称

    @TableField("permission_code")
    private String permissionCode;       // 权限代码

    @TableField("description")
    private String description;          // 权限描述

    @TableField("module")
    private String module;               // 所属模块

    @TableField("create_time")
    private LocalDateTime createTime;    // 创建时间

    @TableField("update_time")
    private LocalDateTime updateTime;    // 更新时间
}