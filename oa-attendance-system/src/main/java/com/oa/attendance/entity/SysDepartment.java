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
 * 部门实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
<Accessors(chain = true)>
@TableName("sys_department")
public class SysDepartment {

    @TableId(value = "dept_id", type = IdType.AUTO)
    private Long deptId;                 // 部门ID

    @TableField("parent_id")
    private Long parentId;               // 父级部门ID

    @TableField("dept_name")
    private String deptName;             // 部门名称

    @TableField("dept_code")
    private String deptCode;             // 部门代码

    @TableField("leader_id")
    private Long leaderId;               // 部门负责人ID

    @TableField("description")
    private String description;          // 部门描述

    @TableField("status")
    private Integer status;              // 状态：1启用 0禁用

    @TableField("deleted")
    private Integer deleted;             // 逻辑删除：0未删除 1已删除

    @TableField("create_time")
    private LocalDateTime createTime;    // 创建时间

    @TableField("update_time")
    private LocalDateTime updateTime;    // 更新时间

    @TableField(exist = false)
    private String leaderName;           // 负责人姓名

    @TableField(exist = false)
    private String childrenCount;        // 子部门数量
}