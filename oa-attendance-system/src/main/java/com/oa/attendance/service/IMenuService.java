package com.oa.attendance.service;

import com.oa.attendance.dto.MenuCreateDTO;
import com.oa.attendance.dto.MenuUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.vo.MenuListVO;
import com.oa.attendance.vo.MenuTreeNodeVO;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface IMenuService {

    /**
     * 创建菜单
     */
    Result<?> create(MenuCreateDTO dto);

    /**
     * 更新菜单
     */
    Result<?> update(MenuUpdateDTO dto);

    /**
     * 删除菜单（软删除）
     */
    Result<?> delete(Long menuId);

    /**
     * 根据ID查询菜单
     */
    Result<MenuListVO> getById(Long id);

    /**
     * 查询所有菜单（列表形式）
     */
    Result<List<MenuListVO>> listAll();

    /**
     * 查询菜单树形结构
     */
    Result<List<MenuTreeNodeVO>> getTree();

    /**
     * 查询当前用户菜单树
     */
    Result<List<MenuTreeNodeVO>> getCurrentUserTree(String username);

    /**
     * 根据父级ID查询子菜单
     */
    Result<List<MenuListVO>> getByParentId(Long parentId);

    /**
     * 根据用户ID查询菜单
     */
    Result<List<MenuListVO>> listByUserId(Long userId);

    /**
     * 根据角色ID查询菜单
     */
    Result<List<MenuListVO>> listByRoleId(Long roleId);
}
