package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.SysPosition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 职位Mapper接口
 */
@Mapper
public interface SysPositionMapper extends BaseMapper<SysPosition> {

    /**
     * 根据部门ID查询职位列表
     */
    @Select("SELECT * FROM sys_position WHERE dept_id = #{deptId} AND deleted = 0 ORDER BY level DESC, create_time DESC")
    List<SysPosition> selectByDeptId(@Param("deptId") Long deptId);

    /**
     * 软删除职位
     */
    @Update("UPDATE sys_position SET deleted = 1 WHERE position_id = #{positionId}")
    int softDeleteById(@Param("positionId") Long positionId);

    /**
     * 根据部门ID查询职位数量
     */
    @Select("SELECT COUNT(*) FROM sys_position WHERE dept_id = #{deptId} AND deleted = 0")
    int countByDeptId(@Param("deptId") Long deptId);
}