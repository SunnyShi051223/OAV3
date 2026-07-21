package com.oa.attendance.service;

import com.oa.attendance.entity.SysRole;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.SysRoleMapper;
import com.oa.attendance.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class DataScopeService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    public boolean hasFullDataAccess() {
        return ROLE_ADMIN.equals(getCurrentRoleCode());
    }

    public boolean hasDepartmentDataAccess() {
        return ROLE_MANAGER.equals(getCurrentRoleCode());
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

    private String getCurrentRoleCode() {
        SysUser user = getCurrentUser();
        if (user == null || user.getRoleId() == null) {
            return null;
        }
        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        return role == null ? null : role.getRoleCode();
    }
}
