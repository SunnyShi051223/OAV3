package com.oa.attendance.service;

import com.oa.attendance.dto.DepartmentCreateDTO;
import com.oa.attendance.dto.DepartmentUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.vo.DepartmentListVO;
import com.oa.attendance.vo.DepartmentTreeNodeVO;

import java.util.List;

/**
 * 部门服务接口
 */
public interface IDepartmentService {

    /**
     * 创建部门
     */
    Result<?> create(DepartmentCreateDTO dto);

    /**
     * 更新部门
     */
    Result<?> update(DepartmentUpdateDTO dto);

    /**
     * 删除部门（软删除）
     */
    Result<?> delete(Long deptId);

    /**
     * 根据ID查询部门
     */
    Result<DepartmentListVO> getById(Long id);

    /**
     * 查询所有部门（列表形式）
     */
    Result<List<DepartmentListVO>> listAll();

    /**
     * 查询部门树形结构
     */
    Result<List<DepartmentTreeNodeVO>> getTree();

    /**
     * 根据父级ID查询子部门
     */
    Result<List<DepartmentListVO>> getByParentId(Long parentId);
}