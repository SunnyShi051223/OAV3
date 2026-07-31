package com.oa.attendance.service;

import com.oa.attendance.entity.SysPermission;
import com.oa.attendance.entity.SysRole;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.SysPermissionMapper;
import com.oa.attendance.mapper.SysRoleMapper;
import com.oa.attendance.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataScopeService {

    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    /**
     * 全公司数据权限 —— 拥有 approval:all 权限的角色
     */
    public boolean hasFullDataAccess() {
        return hasPermission("approval:all");
    }

    /**
     * 部门数据权限 —— 拥有 approval:handle 权限的角色
     */
    public boolean hasDepartmentDataAccess() {
        return hasPermission("approval:handle");
    }

    private boolean hasPermission(String code) {
        SysUser user = getCurrentUser();
        if (user == null || user.getRoleId() == null) return false;
        List<SysPermission> perms = permissionMapper.selectPermissionsByRoleId(user.getRoleId());
        return perms.stream().anyMatch(p -> code.equals(p.getPermissionCode()));
    }

    public Long getCurrentDeptId() {
        SysUser user = getCurrentUser();
        return user == null ? null : user.getDeptId();
    }

    public boolean canAccessDepartment(Long deptId) {
        if (hasFullDataAccess()) {
            return true;
        }
        if (!hasDepartmentDataAccess()) {
            return false;
        }
        Long currentDeptId = getCurrentDeptId();
        return currentDeptId != null && currentDeptId.equals(deptId);
    }

    public boolean canAssignRole(Long roleId) {
        if (hasFullDataAccess()) {
            return true;
        }
        if (!hasDepartmentDataAccess()) {
            return false;
        }
        SysRole role = roleId == null ? null : sysRoleMapper.selectById(roleId);
        return role != null && ROLE_EMPLOYEE.equals(role.getRoleCode());
    }

    public SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return sysUserMapper.findByUsername(authentication.getName());
    }
}
