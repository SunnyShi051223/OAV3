package com.oa.attendance.controller;

import com.oa.attendance.dto.RoleCreateDTO;
import com.oa.attendance.dto.RoleMenuAssignDTO;
import com.oa.attendance.dto.RolePermissionAssignDTO;
import com.oa.attendance.dto.RoleUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysPermission;
import com.oa.attendance.mapper.SysPermissionMapper;
import com.oa.attendance.service.IRoleService;
import com.oa.attendance.vo.RoleListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 角色控制器
 */
@RestController
@RequestMapping("/api/roles")
@Validated
@CrossOrigin
public class RoleController {

    @Autowired
    private IRoleService roleService;

    @Autowired
    private SysPermissionMapper permissionMapper;

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('role:query')")
    public Result<List<SysPermission>> listPermissions() {
        return Result.success("查询成功", permissionMapper.selectList(null));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:add')")
    public Result<?> create(@Valid @RequestBody RoleCreateDTO dto) {
        return roleService.create(dto);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('role:update')")
    public Result<?> update(@Valid @RequestBody RoleUpdateDTO dto) {
        return roleService.update(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public Result<?> delete(@PathVariable Long id) {
        return roleService.delete(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:query')")
    public Result<RoleListVO> getById(@PathVariable Long id) {
        return roleService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:query')")
    public Result<List<RoleListVO>> listAll() {
        return roleService.listAll();
    }

    @PutMapping("/menus")
    @PreAuthorize("hasAuthority('role:menu')")
    public Result<?> assignMenus(@Valid @RequestBody RoleMenuAssignDTO dto) {
        return roleService.assignMenus(dto);
    }

    @PutMapping("/permissions")
    @PreAuthorize("hasAuthority('role:permission')")
    public Result<?> assignPermissions(@Valid @RequestBody RolePermissionAssignDTO dto) {
        return roleService.assignPermissions(dto);
    }
}
