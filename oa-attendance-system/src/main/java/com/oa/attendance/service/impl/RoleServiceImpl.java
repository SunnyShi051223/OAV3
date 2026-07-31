package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.dto.RoleCreateDTO;
import com.oa.attendance.dto.RoleMenuAssignDTO;
import com.oa.attendance.dto.RolePermissionAssignDTO;
import com.oa.attendance.dto.RoleUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysMenu;
import com.oa.attendance.entity.SysPermission;
import com.oa.attendance.entity.SysRole;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.SysMenuMapper;
import com.oa.attendance.mapper.SysPermissionMapper;
import com.oa.attendance.mapper.SysRoleMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.service.IRoleService;
import com.oa.attendance.vo.RoleListVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Service
public class RoleServiceImpl implements IRoleService {

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Override
    @Transactional
    public Result<?> create(RoleCreateDTO dto) {
        if (existsRoleCode(dto.getRoleCode(), null)) {
            return Result.error("角色编码已存在");
        }

        SysRole role = new SysRole();
        BeanUtils.copyProperties(dto, role);
        role.setRoleCode(dto.getRoleCode().toUpperCase());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());

        int result = roleMapper.insert(role);
        return result > 0 ? Result.success("创建成功") : Result.error("创建失败");
    }

    @Override
    @Transactional
    public Result<?> update(RoleUpdateDTO dto) {
        SysRole existing = roleMapper.selectById(dto.getRoleId());
        if (existing == null) {
            return Result.error("角色不存在");
        }
        if (existsRoleCode(dto.getRoleCode(), dto.getRoleId())) {
            return Result.error("角色编码已存在");
        }

        SysRole role = new SysRole();
        BeanUtils.copyProperties(dto, role);
        role.setRoleCode(dto.getRoleCode().toUpperCase());
        role.setUpdateTime(LocalDateTime.now());

        int result = roleMapper.updateById(role);
        return result > 0 ? Result.success("更新成功") : Result.error("更新失败");
    }

    @Override
    @Transactional
    public Result<?> delete(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            return Result.error("角色不存在");
        }

        QueryWrapper<SysUser> userWrapper = new QueryWrapper<>();
        userWrapper.eq("role_id", roleId).eq("deleted", 0);
        if (userMapper.selectCount(userWrapper) > 0) {
            return Result.error("该角色下存在用户，无法删除");
        }

        roleMapper.deleteRoleMenus(roleId);
        roleMapper.deleteRolePermissions(roleId);
        int result = roleMapper.deleteById(roleId);
        return result > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    @Override
    public Result<RoleListVO> getById(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role != null && !dataScopeService.canAssignRole(roleId)) {
            return Result.error("只能查看可分配角色");
        }
        if (role == null) {
            return Result.error("角色不存在");
        }
        return Result.success("查询成功", buildVO(role));
    }

    @Override
    public Result<List<RoleListVO>> listAll() {
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("role_id");
        List<RoleListVO> roles = roleMapper.selectList(wrapper)
                .stream()
                .map(this::buildVO)
                .collect(Collectors.toList());
        return Result.success("查询成功", roles);
    }

    @Override
    @Transactional
    public Result<?> assignMenus(RoleMenuAssignDTO dto) {
        if (roleMapper.selectById(dto.getRoleId()) == null) {
            return Result.error("角色不存在");
        }
        List<Long> menuIds = dto.getMenuIds() == null ? Collections.emptyList() : dto.getMenuIds();
        for (Long menuId : menuIds) {
            SysMenu menu = menuMapper.selectById(menuId);
            if (menu == null || menu.getDeleted() != 0) {
                return Result.error("菜单不存在: " + menuId);
            }
        }

        roleMapper.deleteRoleMenus(dto.getRoleId());
        for (Long menuId : menuIds) {
            roleMapper.insertRoleMenu(dto.getRoleId(), menuId);
        }
        return Result.success("菜单授权成功");
    }

    @Override
    @Transactional
    public Result<?> assignPermissions(RolePermissionAssignDTO dto) {
        if (roleMapper.selectById(dto.getRoleId()) == null) {
            return Result.error("角色不存在");
        }
        List<Long> permissionIds = dto.getPermissionIds() == null ? Collections.emptyList() : dto.getPermissionIds();
        for (Long permissionId : permissionIds) {
            SysPermission permission = permissionMapper.selectById(permissionId);
            if (permission == null) {
                return Result.error("权限不存在: " + permissionId);
            }
        }

        roleMapper.deleteRolePermissions(dto.getRoleId());
        for (Long permissionId : permissionIds) {
            roleMapper.insertRolePermission(dto.getRoleId(), permissionId);
        }
        return Result.success("权限授权成功");
    }

    private boolean existsRoleCode(String roleCode, Long excludeRoleId) {
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        wrapper.eq("role_code", roleCode.toUpperCase());
        if (excludeRoleId != null) {
            wrapper.ne("role_id", excludeRoleId);
        }
        return roleMapper.selectCount(wrapper) > 0;
    }

    private RoleListVO buildVO(SysRole role) {
        RoleListVO vo = new RoleListVO();
        BeanUtils.copyProperties(role, vo);
        vo.setMenuIds(roleMapper.selectMenuIdsByRoleId(role.getRoleId()));
        vo.setPermissionIds(roleMapper.selectPermissionIdsByRoleId(role.getRoleId()));
        return vo;
    }
}
