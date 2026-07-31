package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.oa.attendance.dto.DepartmentCreateDTO;
import com.oa.attendance.dto.DepartmentUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysDepartment;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.exception.BusinessException;
import com.oa.attendance.mapper.SysDepartmentMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.service.IDepartmentService;
import com.oa.attendance.vo.DepartmentListVO;
import com.oa.attendance.vo.DepartmentTreeNodeVO;
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
 * 部门服务实现类
 */
@Service
public class DepartmentServiceImpl implements IDepartmentService {

    @Autowired
    private SysDepartmentMapper departmentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Override
    @Transactional
    public Result<?> create(DepartmentCreateDTO dto) {
        if (!dataScopeService.hasFullDataAccess()) {
            return Result.error("部门主管只能维护本部门");
        }

        // 检查部门代码是否已存在
        QueryWrapper<SysDepartment> wrapper = new QueryWrapper<>();
        wrapper.eq("dept_code", dto.getDeptCode());
        wrapper.eq("deleted", 0);
        Long count = departmentMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("部门代码已存在");
        }

        SysDepartment department = new SysDepartment();
        BeanUtils.copyProperties(dto, department);
        department.setDeleted(0);
        department.setCreateTime(LocalDateTime.now());
        department.setUpdateTime(LocalDateTime.now());

        int result = departmentMapper.insert(department);
        if (result > 0) {
            return Result.success("创建成功");
        }
        return Result.error("创建失败");
    }

    @Override
    @Transactional
    public Result<?> update(DepartmentUpdateDTO dto) {
        SysDepartment existing = departmentMapper.selectById(dto.getDeptId());
        if (existing != null && !dataScopeService.canAccessDepartment(existing.getDeptId())) {
            return Result.error("只能维护本部门");
        }
        if (existing == null) {
            return Result.error("部门不存在");
        }

        // 检查部门代码是否与其他部门冲突（排除当前部门）
        QueryWrapper<SysDepartment> wrapper = new QueryWrapper<>();
        wrapper.eq("dept_code", dto.getDeptCode());
        wrapper.eq("deleted", 0);
        wrapper.ne("dept_id", dto.getDeptId());
        Long count = departmentMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("部门代码已存在");
        }

        UpdateWrapper<SysDepartment> uw = new UpdateWrapper<>();
        uw.eq("dept_id", dto.getDeptId())
          .set("parent_id", dto.getParentId())
          .set("dept_name", dto.getDeptName())
          .set("dept_code", dto.getDeptCode())
          .set("leader_id", dto.getLeaderId())
          .set("description", dto.getDescription())
          .set("status", dto.getStatus())
          .set("update_time", LocalDateTime.now());

        departmentMapper.update(null, uw);
        return Result.success("更新成功");
    }

    @Override
    @Transactional
    public Result<?> delete(Long deptId) {
        SysDepartment dept = departmentMapper.selectById(deptId);
        if (dept != null && !dataScopeService.canAccessDepartment(dept.getDeptId())) {
            return Result.error("只能删除本部门");
        }

        if (dept == null) {
            throw new BusinessException("部门不存在");
        }

        // 检查是否有子部门
        QueryWrapper<SysDepartment> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", deptId);
        wrapper.eq("deleted", 0);
        Long childCount = departmentMapper.selectCount(wrapper);
        if (childCount > 0) {
            return Result.error("该部门存在子部门，无法删除");
        }

        // 检查是否有用户隶属于此部门
        QueryWrapper<SysUser> userWrapper = new QueryWrapper<>();
        userWrapper.eq("dept_id", deptId);
        userWrapper.eq("deleted", 0);
        Long userCount = sysUserMapper.selectCount(userWrapper);
        if (userCount > 0) {
            return Result.error("该部门下存在用户，无法删除");
        }

        dept.setDeleted(1);
        dept.setUpdateTime(LocalDateTime.now());

        int result = departmentMapper.updateById(dept);
        if (result > 0) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    @Override
    public Result<DepartmentListVO> getById(Long id) {
        SysDepartment dept = departmentMapper.selectById(id);
        if (dept != null && dept.getDeleted() == 0) {
            if (!dataScopeService.canAccessDepartment(dept.getDeptId())) {
                return Result.error("只能查看本部门");
            }
            DepartmentListVO vo = new DepartmentListVO();
            BeanUtils.copyProperties(dept, vo);

            // 如果有父级部门，查询父级部门名称
            if (dept.getParentId() != null && dept.getParentId() != 0L) {
                SysDepartment parent = departmentMapper.selectById(dept.getParentId());
                if (parent != null) {
                    vo.setParentDeptName(parent.getDeptName());
                }
            }

            return Result.success("查询成功", vo);
        }
        return Result.error("部门不存在");
    }

    @Override
    public Result<List<DepartmentListVO>> listAll() {
        QueryWrapper<SysDepartment> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        if (dataScopeService.hasDepartmentDataAccess() && !dataScopeService.hasFullDataAccess()) {
            Long currentDeptId = dataScopeService.getCurrentDeptId();
            if (currentDeptId == null) {
                return Result.success("查询成功", new ArrayList<>());
            }
            List<Long> deptIds = new ArrayList<>();
            deptIds.add(currentDeptId);
            collectDescendantDeptIds(currentDeptId, deptIds);
            wrapper.in("dept_id", deptIds);
        }
        wrapper.orderByDesc("create_time");

        List<SysDepartment> departments = departmentMapper.selectList(wrapper);
        List<DepartmentListVO> vos = departments.stream().map(dept -> {
            DepartmentListVO vo = new DepartmentListVO();
            BeanUtils.copyProperties(dept, vo);

            // 查询父级部门名称
            if (dept.getParentId() != null && dept.getParentId() != 0L) {
                SysDepartment parent = departmentMapper.selectById(dept.getParentId());
                if (parent != null) {
                    vo.setParentDeptName(parent.getDeptName());
                }
            }
            // 查询负责人名称
            if (dept.getLeaderId() != null) {
                SysUser leader = sysUserMapper.selectById(dept.getLeaderId());
                if (leader != null) {
                    vo.setLeaderName(leader.getRealName());
                }
            }

            return vo;
        }).collect(Collectors.toList());

        return Result.success("查询成功", vos);
    }

    @Override
    public Result<List<DepartmentTreeNodeVO>> getTree() {
        // 查询所有部门
        QueryWrapper<SysDepartment> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        wrapper.orderByAsc("parent_id");
        List<SysDepartment> allDepts = departmentMapper.selectList(wrapper);

        if (dataScopeService.hasDepartmentDataAccess() && !dataScopeService.hasFullDataAccess()) {
            // 部门级权限：只显示当前部门及其子部门
            Long currentDeptId = dataScopeService.getCurrentDeptId();
            if (currentDeptId == null) {
                return Result.success("查询成功", new ArrayList<>());
            }
            List<SysDepartment> visible = new ArrayList<>();
            collectDeptAndDescendants(currentDeptId, allDepts, visible);
            List<DepartmentTreeNodeVO> tree = buildTree(visible);
            return Result.success("查询成功", tree);
        }

        // 管理员：显示全部
        List<DepartmentTreeNodeVO> treeNodes = buildTree(allDepts);
        return Result.success("查询成功", treeNodes);
    }

    private void collectDeptAndDescendants(Long deptId, List<SysDepartment> allDepts, List<SysDepartment> result) {
        for (SysDepartment dept : allDepts) {
            if (deptId.equals(dept.getDeptId())) {
                result.add(dept);
                // 递归收集子部门
                for (SysDepartment child : allDepts) {
                    if (deptId.equals(child.getParentId())) {
                        collectDeptAndDescendants(child.getDeptId(), allDepts, result);
                    }
                }
                break;
            }
        }
    }

    @Override
    public Result<List<DepartmentListVO>> getByParentId(Long parentId) {
        if (!dataScopeService.canAccessDepartment(parentId)) {
            return Result.error("只能查看本部门");
        }
        if (dataScopeService.hasDepartmentDataAccess()) {
            return Result.success("查询成功", new ArrayList<>());
        }

        List<SysDepartment> departments = departmentMapper.selectByParentId(parentId);
        List<DepartmentListVO> vos = departments.stream().map(dept -> {
            DepartmentListVO vo = new DepartmentListVO();
            BeanUtils.copyProperties(dept, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.success("查询成功", vos);
    }

    /**
     * 构建部门树形结构
     */
    private List<DepartmentTreeNodeVO> buildTree(List<SysDepartment> departments) {
        // 创建Map便于快速查找
        Map<Long, DepartmentTreeNodeVO> nodeMap = new HashMap<>();
        List<DepartmentTreeNodeVO> rootNodes = new ArrayList<>();

        // 第一步：创建所有节点并放入Map
        for (SysDepartment dept : departments) {
            DepartmentTreeNodeVO node = new DepartmentTreeNodeVO();
            BeanUtils.copyProperties(dept, node);
            nodeMap.put(dept.getDeptId(), node);
        }

        // 第二步：建立父子关系
        for (SysDepartment dept : departments) {
            DepartmentTreeNodeVO currentNode = nodeMap.get(dept.getDeptId());

            if (dept.getParentId() == null || dept.getParentId() == 0L) {
                // 根节点
                rootNodes.add(currentNode);
            } else {
                // 查找父节点
                DepartmentTreeNodeVO parentNode = nodeMap.get(dept.getParentId());
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

    private void collectDescendantDeptIds(Long parentId, List<Long> result) {
        List<SysDepartment> children = departmentMapper.selectByParentId(parentId);
        for (SysDepartment child : children) {
            result.add(child.getDeptId());
            collectDescendantDeptIds(child.getDeptId(), result);
        }
    }
}
