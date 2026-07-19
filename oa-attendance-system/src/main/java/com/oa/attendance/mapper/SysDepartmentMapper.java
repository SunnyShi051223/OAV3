package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.SysDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 部门Mapper接口
 */
@Mapper
public interface SysDepartmentMapper extends BaseMapper<SysDepartment> {

    /**
     * 根据父级部门ID查询子部门列表
     */
    @Select("SELECT * FROM sys_department WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY create_time DESC")
    List<SysDepartment> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 软删除部门
     */
    @Update("UPDATE sys_department SET deleted = 1 WHERE dept_id = #{deptId}")
    int softDeleteById(@Param("deptId") Long deptId);
}