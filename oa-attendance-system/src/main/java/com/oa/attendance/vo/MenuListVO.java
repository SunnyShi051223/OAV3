package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单列表项VO
 */
@Data
public class MenuListVO {

    private Long menuId;                 // 菜单ID
    private Long parentId;               // 父级菜单ID
    private String menuName;             // 菜单名称
    private String menuPath;             // 菜单路径
    private String component;            // 组件路径
    private String icon;                 // 图标
    private Integer sortOrder;           // 排序
    private Integer visible;             // 是否可见
    private String parentMenuName;       // 父级菜单名称
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间
}