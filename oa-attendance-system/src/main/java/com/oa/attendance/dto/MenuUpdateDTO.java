package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 菜单更新DTO
 */
@Data
public class MenuUpdateDTO {

    @NotNull(message = "菜单ID不能为空")
    private Long menuId;                 // 菜单ID

    @NotNull(message = "父级菜单ID不能为空")
    private Long parentId;               // 父级菜单ID

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;             // 菜单名称

    private String menuPath;             // 菜单路径

    private String component;            // 组件路径

    private String icon;                 // 图标

    @NotNull(message = "排序不能为空")
    private Integer sortOrder;           // 排序

    @NotNull(message = "可见性不能为空")
    private Integer visible;             // 是否可见
}