package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名或工号查询用户
     */
    @Select("SELECT * FROM sys_user WHERE (username = #{username} OR employee_no = #{username}) AND deleted = 0")
    SysUser findByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM sys_user WHERE employee_no = #{employeeNo}")
    long countByEmployeeNoIncludingDeleted(@Param("employeeNo") String employeeNo);

    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username}")
    long countByUsernameIncludingDeleted(@Param("username") String username);

    /**
     * 根据用户ID查询用户详细信息
     */
    @Select("SELECT u.*, r.role_name, d.dept_name, p.position_name " +
            "FROM sys_user u " +
            "LEFT JOIN sys_role r ON u.role_id = r.role_id " +
            "LEFT JOIN sys_department d ON u.dept_id = d.dept_id " +
            "LEFT JOIN sys_position p ON u.position_id = p.position_id " +
            "WHERE u.user_id = #{userId} AND u.deleted = 0")
    SysUser getUserDetailById(@Param("userId") Long userId);

    /**
     * 根据部门ID查询用户列表
     */
    @Select("SELECT u.*, r.role_name, d.dept_name, p.position_name " +
            "FROM sys_user u " +
            "LEFT JOIN sys_role r ON u.role_id = r.role_id " +
            "LEFT JOIN sys_department d ON u.dept_id = d.dept_id " +
            "LEFT JOIN sys_position p ON u.position_id = p.position_id " +
            "WHERE u.dept_id = #{deptId} AND u.deleted = 0 " +
            "ORDER BY u.create_time DESC")
    List<SysUser> selectByDeptId(@Param("deptId") Long deptId);

    /**
     * 根据关键词搜索同事（姓名、工号、手机号），包含角色和部门信息
     */
    @Select("SELECT u.*, r.role_name, r.role_code, d.dept_name, p.position_name " +
            "FROM sys_user u " +
            "LEFT JOIN sys_role r ON u.role_id = r.role_id " +
            "LEFT JOIN sys_department d ON u.dept_id = d.dept_id " +
            "LEFT JOIN sys_position p ON u.position_id = p.position_id " +
            "WHERE u.deleted = 0 AND u.status = 1 " +
            "AND (u.real_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR u.employee_no LIKE CONCAT('%', #{keyword}, '%') " +
            "OR u.phone LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY u.dept_id, u.create_time DESC")
    List<SysUser> searchColleagues(@Param("keyword") String keyword);

    /**
     * 查询所有在职用户（含部门、职位、角色信息）
     */
    @Select("SELECT u.*, r.role_name, r.role_code, d.dept_name, p.position_name " +
            "FROM sys_user u " +
            "LEFT JOIN sys_role r ON u.role_id = r.role_id " +
            "LEFT JOIN sys_department d ON u.dept_id = d.dept_id " +
            "LEFT JOIN sys_position p ON u.position_id = p.position_id " +
            "WHERE u.deleted = 0 AND u.status = 1 " +
            "ORDER BY u.dept_id, u.real_name")
    List<SysUser> selectAllActiveWithDetails();

    /**
     * 软删除用户
     */
    @Update("UPDATE sys_user SET deleted = 1 WHERE user_id = #{userId}")
    int softDeleteById(@Param("userId") Long userId);
}
