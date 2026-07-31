package com.oa.attendance.controller;

import com.oa.attendance.dto.DepartmentCreateDTO;
import com.oa.attendance.dto.DepartmentUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IDepartmentService;
import com.oa.attendance.vo.DepartmentListVO;
import com.oa.attendance.vo.DepartmentTreeNodeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 部门控制器
 */
@RestController
@RequestMapping("/api/departments")
@Validated
@CrossOrigin
public class DepartmentController {

    @Autowired
    private IDepartmentService departmentService;

    /**
     * 创建部门
     */
    @PostMapping
    @PreAuthorize("hasAuthority('department:add')")
    public Result<?> create(@Valid @RequestBody DepartmentCreateDTO dto) {
        return departmentService.create(dto);
    }

    /**
     * 更新部门
     */
    @PutMapping
    @PreAuthorize("hasAuthority('department:update')")
    public Result<?> update(@Valid @RequestBody DepartmentUpdateDTO dto) {
        return departmentService.update(dto);
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('department:delete')")
    public Result<?> delete(@PathVariable Long id) {
        return departmentService.delete(id);
    }

    /**
     * 根据ID查询部门
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('department:query')")
    public Result<DepartmentListVO> getById(@PathVariable Long id) {
        return departmentService.getById(id);
    }

    /**
     * 查询所有部门
     */
    @GetMapping
    @PreAuthorize("hasAuthority('department:query')")
    public Result<List<DepartmentListVO>> listAll() {
        return departmentService.listAll();
    }

    /**
     * 查询部门树形结构
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('department:query')")
    public Result<List<DepartmentTreeNodeVO>> getTree() {
        return departmentService.getTree();
    }

    /**
     * 根据父级ID查询子部门
     */
    @GetMapping("/children/{parentId}")
    @PreAuthorize("hasAuthority('department:query')")
    public Result<List<DepartmentListVO>> getByParentId(@PathVariable Long parentId) {
        return departmentService.getByParentId(parentId);
    }
}