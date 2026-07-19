package com.oa.attendance.controller;

import com.oa.attendance.dto.MenuCreateDTO;
import com.oa.attendance.dto.MenuUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IMenuService;
import com.oa.attendance.vo.MenuListVO;
import com.oa.attendance.vo.MenuTreeNodeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 菜单控制器
 */
@RestController
@RequestMapping("/api/menus")
@Validated
@CrossOrigin
public class MenuController {

    @Autowired
    private IMenuService menuService;

    /**
     * 创建菜单
     */
    @PostMapping
    @PreAuthorize("hasAuthority('menu:add')")
    public Result<?> create(@Valid @RequestBody MenuCreateDTO dto) {
        return menuService.create(dto);
    }

    /**
     * 更新菜单
     */
    @PutMapping
    @PreAuthorize("hasAuthority('menu:update')")
    public Result<?> update(@Valid @RequestBody MenuUpdateDTO dto) {
        return menuService.update(dto);
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:delete')")
    public Result<?> delete(@PathVariable Long id) {
        return menuService.delete(id);
    }

    /**
     * 根据ID查询菜单
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:query')")
    public Result<MenuListVO> getById(@PathVariable Long id) {
        return menuService.getById(id);
    }

    /**
     * 查询所有菜单
     */
    @GetMapping
    @PreAuthorize("hasAuthority('menu:query')")
    public Result<List<MenuListVO>> listAll() {
        return menuService.listAll();
    }

    /**
     * 查询菜单树形结构
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('menu:query')")
    public Result<List<MenuTreeNodeVO>> getTree() {
        return menuService.getTree();
    }

    /**
     * 根据父级ID查询子菜单
     */
    @GetMapping("/children/{parentId}")
    @PreAuthorize("hasAuthority('menu:query')")
    public Result<List<MenuListVO>> getByParentId(@PathVariable Long parentId) {
        return menuService.getByParentId(parentId);
    }

    /**
     * 根据用户ID查询菜单
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('menu:query')")
    public Result<List<MenuListVO>> listByUserId(@PathVariable Long userId) {
        return menuService.listByUserId(userId);
    }

    /**
     * 根据角色ID查询菜单
     */
    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasAuthority('menu:query')")
    public Result<List<MenuListVO>> listByRoleId(@PathVariable Long roleId) {
        return menuService.listByRoleId(roleId);
    }
}