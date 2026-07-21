package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色列表VO
 */
@Data
public class RoleListVO {

    private Long roleId;
    private String roleName;
    private String roleCode;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<Long> menuIds;
    private List<Long> permissionIds;
}
