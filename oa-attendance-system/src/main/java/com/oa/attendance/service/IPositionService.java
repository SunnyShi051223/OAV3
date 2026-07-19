package com.oa.attendance.service;

import com.oa.attendance.dto.PositionCreateDTO;
import com.oa.attendance.dto.PositionUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.vo.PositionListVO;

import java.util.List;

/**
 * 职位服务接口
 */
public interface IPositionService {

    /**
     * 创建职位
     */
    Result<?> create(PositionCreateDTO dto);

    /**
     * 更新职位
     */
    Result<?> update(PositionUpdateDTO dto);

    /**
     * 删除职位（软删除）
     */
    Result<?> delete(Long positionId);

    /**
     * 根据ID查询职位
     */
    Result<PositionListVO> getById(Long id);

    /**
     * 查询所有职位
     */
    Result<List<PositionListVO>> listAll();

    /**
     * 根据部门ID查询职位列表
     */
    Result<List<PositionListVO>> listByDeptId(Long deptId);
}