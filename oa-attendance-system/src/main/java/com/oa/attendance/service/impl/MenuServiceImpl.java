package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.dto.MenuCreateDTO;
import com.oa.attendance.dto.MenuUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysMenu;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.exception.BusinessException;
import com.oa.attendance.mapper.SysMenuMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.IMenuService;
import com.oa.attendance.vo.MenuListVO;
import com.oa.attendance.vo.MenuTreeNodeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 */
@Service
public class MenuServiceImpl implements IMenuService {

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Override
    @Transactional
    public Result<?> create(MenuCreateDTO dto) {
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(dto, menu);
        menu.setDeleted(0);
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());

        int result = menuMapper.insert(menu);
        if (result > 0) {
            return Result.success("创建成功");
        }
        return Result.error("创建失败");
    }

    @Override
    @Transactional
    public Result<?> update(MenuUpdateDTO dto) {
        SysMenu existing = menuMapper.selectById(dto.getMenuId());
        if (existing == null) {
            return Result.error("菜单不存在");
        }

        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(dto, menu);
        menu.setUpdateTime(LocalDateTime.now());

        int result = menuMapper.updateById(menu);
        if (result > 0) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    @Override
    @Transactional
    public Result<?> delete(Long menuId) {
        SysMenu menu = menuMapper.selectById(menuId);

        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }

        // 检查是否有子菜单
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", menuId);
        wrapper.eq("deleted", 0);
        Long childCount = menuMapper.selectCount(wrapper);
        if (childCount > 0) {
            return Result.error("该菜单存在子菜单，无法删除");
        }

        int result = menuMapper.softDeleteById(menuId);
        if (result > 0) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    @Override
    public Result<MenuListVO> getById(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu != null && menu.getDeleted() == 0) {
            MenuListVO vo = new MenuListVO();
            BeanUtils.copyProperties(menu, vo);

            return Result.success("查询成功", vo);
        }
        return Result.error("菜单不存在");
    }

    @Override
    public Result<List<MenuListVO>> listAll() {
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        wrapper.orderByAsc("parent_id").orderByAsc("sort_order");

        List<SysMenu> menus = menuMapper.selectList(wrapper);
        List<MenuListVO> vos = menus.stream().map(menu -> {
            MenuListVO vo = new MenuListVO();
            BeanUtils.copyProperties(menu, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.success("查询成功", vos);
    }

    @Override
    public Result<List<MenuTreeNodeVO>> getTree() {
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        wrapper.orderByAsc("parent_id").orderByAsc("sort_order");

        List<SysMenu> menus = menuMapper.selectList(wrapper);

        // 构建树形结构
        List<MenuTreeNodeVO> treeNodes = buildTree(menus);

        return Result.success("查询成功", treeNodes);
    }

    @Override
    public Result<List<MenuTreeNodeVO>> getCurrentUserTree(String username) {
        SysUser user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error("当前用户不存在");
        }

        List<SysMenu> menus = menuMapper.selectMenusByUserId(user.getUserId());
        return Result.success("查询成功", buildTree(menus));
    }

    @Override
    public Result<List<MenuListVO>> getByParentId(Long parentId) {
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", parentId);
        wrapper.eq("deleted", 0);
        wrapper.orderByAsc("sort_order");

        List<SysMenu> menus = menuMapper.selectList(wrapper);
        List<MenuListVO> vos = menus.stream().map(menu -> {
            MenuListVO vo = new MenuListVO();
            BeanUtils.copyProperties(menu, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.success("查询成功", vos);
    }

    @Override
    public Result<List<MenuListVO>> listByUserId(Long userId) {
        List<SysMenu> menus = menuMapper.selectMenusByUserId(userId);
        List<MenuListVO> vos = menus.stream().map(menu -> {
            MenuListVO vo = new MenuListVO();
            BeanUtils.copyProperties(menu, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.success("查询成功", vos);
    }

    @Override
    public Result<List<MenuListVO>> listByRoleId(Long roleId) {
        List<SysMenu> menus = menuMapper.selectMenusByRoleId(roleId);
        List<MenuListVO> vos = menus.stream().map(menu -> {
            MenuListVO vo = new MenuListVO();
            BeanUtils.copyProperties(menu, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.success("查询成功", vos);
    }

    /**
     * 构建菜单树形结构
     */
    private List<MenuTreeNodeVO> buildTree(List<SysMenu> menus) {
        // 创建Map便于快速查找
        Map<Long, MenuTreeNodeVO> nodeMap = new HashMap<>();
        List<MenuTreeNodeVO> rootNodes = new ArrayList<>();

        // 第一步：创建所有节点并放入Map
        for (SysMenu menu : menus) {
            MenuTreeNodeVO node = new MenuTreeNodeVO();
            BeanUtils.copyProperties(menu, node);
            nodeMap.put(menu.getMenuId(), node);
        }

        // 第二步：建立父子关系
        for (SysMenu menu : menus) {
            MenuTreeNodeVO currentNode = nodeMap.get(menu.getMenuId());

            if (menu.getParentId() == null || menu.getParentId() == 0) {
                // 根节点
                rootNodes.add(currentNode);
            } else {
                // 查找父节点
                MenuTreeNodeVO parentNode = nodeMap.get(menu.getParentId());
                if (parentNode != null) {
                    if (parentNode.getChildren() == null) {
                        parentNode.setChildren(new ArrayList<>());
                    }
                    parentNode.getChildren().add(currentNode);
                }
            }
        }

        return rootNodes;
    }
}
