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
 * 角色实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_role")
public class SysRole {

    @TableId(value = "role_id", type = IdType.AUTO)
    private Long roleId;                 // 角色ID

    @TableField("role_name")
    private String roleName;             // 角色名称

    @TableField("role_code")
    private String roleCode;             // 角色代码

    @TableField("description")
    private String description;          // 角色描述

    @TableField("create_time")
    private LocalDateTime createTime;    // 创建时间

    @TableField("update_time")
    private LocalDateTime updateTime;    // 更新时间
}