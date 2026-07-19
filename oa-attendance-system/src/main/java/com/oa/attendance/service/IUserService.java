package com.oa.attendance.service;

import com.oa.attendance.dto.UserCreateDTO;
import com.oa.attendance.dto.UserUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.vo.UserListVO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface IUserService {

    /**
     * 用户登录
     */
    Result<?> login(String username, String password);

    /**
     * 获取当前用户信息
     */
    Result<?> getCurrentUserInfo();

    /**
     * 创建用户
     */
    Result<?> create(UserCreateDTO dto);

    /**
     * 更新用户
     */
    Result<?> update(UserUpdateDTO dto);

    /**
     * 删除用户（软删除）
     */
    Result<?> delete(Long userId);

    /**
     * 根据ID查询用户
     */
    Result<UserListVO> getById(Long id);

    /**
     * 查询所有用户（列表形式）
     */
    Result<List<UserListVO>> listAll();

    /**
     * 根据部门ID查询用户列表
     */
    Result<List<UserListVO>> listByDeptId(Long deptId);

    /**
     * 更新用户信息
     */
    Result<?> updateUser(UserListVO user);

    /**
     * 修改密码
     */
    Result<?> changePassword(Long userId, String oldPassword, String newPassword);
}