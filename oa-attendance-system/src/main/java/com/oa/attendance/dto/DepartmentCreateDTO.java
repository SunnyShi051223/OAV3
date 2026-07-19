package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 部门创建DTO
 */
@Data
public class DepartmentCreateDTO {

    @NotNull(message = "父级部门ID不能为空")
    private Long parentId;               // 父级部门ID

    @NotBlank(message = "部门名称不能为空")
    private String deptName;             // 部门名称

    @NotBlank(message = "部门代码不能为空")
    private String deptCode;             // 部门代码

    private Long leaderId;               // 部门负责人ID

    private String description;          // 部门描述

    private Integer status;              // 状态：1启用 0禁用
}