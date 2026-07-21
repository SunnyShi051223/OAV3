package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 角色权限授权DTO
 */
@Data
public class RolePermissionAssignDTO {

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    private List<Long> permissionIds;
}
