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
 * 菜单实体类
 */
@Data
<EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_menu")
public class SysMenu {

    @TableId(value = "menu_id", type = IdType.AUTO)
    private Long menuId;                 // 菜单ID

    @TableField("parent_id")
    private Long parentId;               // 父级菜单ID

    @TableField("menu_name")
    private String menuName;             // 菜单名称

    @TableField("menu_path")
    private String menuPath;             // 菜单路径

    @TableField("component")
    private String component;            // 组件路径

    @TableField("icon")
    private String icon;                 // 图标

    @TableField("sort_order")
    private Integer sortOrder;           // 排序

    @TableField("visible")
    private Integer visible;             // 是否可见

    @TableField("create_time")
    private LocalDateTime createTime;    // 创建时间

    @TableField("update_time")
    private LocalDateTime updateTime;    // 更新时间
}