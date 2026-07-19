package com.oa.attendance.controller;

import com.oa.attendance.dto.PositionCreateDTO;
import com.oa.attendance.dto.PositionUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IPositionService;
import com.oa.attendance.vo.PositionListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 职位控制器
 */
@RestController
@RequestMapping("/api/positions")
@Validated
@CrossOrigin
public class PositionController {

    @Autowired
    private IPositionService positionService;

    /**
     * 创建职位
     */
    @PostMapping
    @PreAuthorize("hasAuthority('position:add')")
    public Result<?> create(@Valid @RequestBody PositionCreateDTO dto) {
        return positionService.create(dto);
    }

    /**
     * 更新职位
     */
    @PutMapping
    @PreAuthorize("hasAuthority('position:update')")
    public Result<?> update(@Valid @RequestBody PositionUpdateDTO dto) {
        return positionService.update(dto);
    }

    /**
     * 删除职位
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('position:delete')")
    public Result<?> delete(@PathVariable Long id) {
        return positionService.delete(id);
    }

    /**
     * 根据ID查询职位
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('position:query')")
    public Result<PositionListVO> getById(@PathVariable Long id) {
        return positionService.getById(id);
    }

    /**
     * 查询所有职位
     */
    @GetMapping
    @PreAuthorize("hasAuthority('position:query')")
    public Result<List<PositionListVO>> listAll() {
        return positionService.listAll();
    }

    /**
     * 根据部门ID查询职位列表
     */
    @GetMapping("/dept/{deptId}")
    @PreAuthorize("hasAuthority('position:query')")
    public Result<List<PositionListVO>> listByDeptId(@PathVariable Long deptId) {
        return positionService.listByDeptId(deptId);
    }
}