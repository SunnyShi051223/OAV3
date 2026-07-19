package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名或工号查询用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} OR employee_no = #{username}")
    SysUser findByUsername(@Param("username") String username);

    /**
     * 根据用户ID查询用户详细信息
     */
    @Select("SELECT u.*, r.role_name, d.dept_name, p.position_name " +
            "FROM sys_user u " +
            "LEFT JOIN sys_role r ON u.role_id = r.role_id " +
            "LEFT JOIN sys_department d ON u.dept_id = d.dept_id " +
            "LEFT JOIN sys_position p ON u.position_id = p.position_id " +
            "WHERE u.user_id = #{userId}")
    SysUser getUserDetailById(@Param("userId") Long userId);
}