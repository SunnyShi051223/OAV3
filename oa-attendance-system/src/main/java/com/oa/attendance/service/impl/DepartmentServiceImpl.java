package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oa.attendance.dto.DepartmentCreateDTO;
import com.oa.attendance.dto.DepartmentUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysDepartment;
import com.oa.attendance.exception.BusinessException;
import com.oa.attendance.mapper.SysDepartmentMapper;
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

    @Override
    @Transactional
    public Result<?> create(DepartmentCreateDTO dto) {
        // 检查部门代码是否已存在
        LambdaQueryWrapper<SysDepartment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysDepartment::getDeptCode, dto.getDeptCode()).eq(SysDepartment::getDeleted, 0);
        int count = departmentMapper.selectCount(wrapper);
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
        if (existing == null) {
            return Result.error("部门不存在");
        }

        // 检查部门代码是否与其他部门冲突（排除当前部门）
        LambdaQueryWrapper<SysDepartment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysDepartment::getDeptCode, dto.getDeptCode())
                .eq(SysDepartment::getDeleted, 0)
                .ne(SysDepartment::getDeptId, dto.getDeptId());
        int count = departmentMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("部门代码已存在");
        }

        SysDepartment department = new SysDepartment();
        BeanUtils.copyProperties(dto, department);
        department.setUpdateTime(LocalDateTime.now());

        int result = departmentMapper.updateById(department);
        if (result > 0) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    @Override
    @Transactional
    public Result<?> delete(Long deptId) {
        SysDepartment dept = departmentMapper.selectById(deptId);

        if (dept == null) {
            throw new BusinessException("部门不存在");
        }

        // 检查是否有子部门
        LambdaQueryWrapper<SysDepartment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysDepartment::getParentId, deptId).eq(SysDepartment::getDeleted, 0);
        int childCount = departmentMapper.selectCount(wrapper);
        if (childCount > 0) {
            return Result.error("该部门存在子部门，无法删除");
        }

        // 检查是否有用户隶属于此部门
        // 注意：这里需要引用用户Mapper来检查是否有用户在该部门
        // 由于循环依赖问题，此处简化处理，实际项目中可通过其他方式解决

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
            DepartmentListVO vo = new DepartmentListVO();
            BeanUtils.copyProperties(dept, vo);

            // 如果有父级部门，查询父级部门名称
            if (dept.getParentId() != null && dept.getParentId() != 0) {
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
        LambdaQueryWrapper<SysDepartment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysDepartment::getDeleted, 0).orderByDesc(SysDepartment::getCreateTime);

        List<SysDepartment> departments = departmentMapper.selectList(wrapper);
        List<DepartmentListVO> vos = departments.stream().map(dept -> {
            DepartmentListVO vo = new DepartmentListVO();
            BeanUtils.copyProperties(dept, vo);

            // 查询父级部门名称
            if (dept.getParentId() != null && dept.getParentId() != 0) {
                SysDepartment parent = departmentMapper.selectById(dept.getParentId());
                if (parent != null) {
                    vo.setParentDeptName(parent.getDeptName());
                }
            }

            return vo;
        }).collect(Collectors.toList());

        return Result.success("查询成功", vos);
    }

    @Override
    public Result<List<DepartmentTreeNodeVO>> getTree() {
        // 查询所有未删除的部门
        LambdaQueryWrapper<SysDepartment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysDepartment::getDeleted, 0).orderByAsc(SysDepartment::getParentId);

        List<SysDepartment> departments = departmentMapper.selectList(wrapper);

        // 构建树形结构
        List<DepartmentTreeNodeVO> treeNodes = buildTree(departments);

        return Result.success("查询成功", treeNodes);
    }

    @Override
    public Result<List<DepartmentListVO>> getByParentId(Long parentId) {
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

            if (dept.getParentId() == null || dept.getParentId() == 0) {
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
}