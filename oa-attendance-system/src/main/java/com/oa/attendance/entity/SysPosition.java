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
 * 职位实体类
 */
@Data
<EqualsAndHashCode(callSuper = false)>
<Accessors(chain = true)>
@TableName("sys_position")
public class SysPosition {

    @TableId(value = "position_id", type = IdType.AUTO)
    private Long positionId;             // 职位ID

    @TableField("position_name")
    private String positionName;         // 职位名称

    @TableField("dept_id")
    private Long deptId;                 // 所属部门ID

    @TableField("level")
    private Integer level;               // 职位等级

    @TableField("description")
    private String description;          // 职位描述

    @TableField("deleted")
    private Integer deleted;             // 逻辑删除：0未删除 1已删除

    @TableField("create_time")
    private LocalDateTime createTime;    // 创建时间

    @TableField("update_time")
    private LocalDateTime updateTime;    // 更新时间

    @TableField(exist = false)
    private String deptName;             // 部门名称
}