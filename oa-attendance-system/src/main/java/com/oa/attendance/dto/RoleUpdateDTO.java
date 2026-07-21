package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 角色更新DTO
 */
@Data
public class RoleUpdateDTO {

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称不能超过50个字符")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码不能超过50个字符")
    private String roleCode;

    @Size(max = 255, message = "角色描述不能超过255个字符")
    private String description;
}
