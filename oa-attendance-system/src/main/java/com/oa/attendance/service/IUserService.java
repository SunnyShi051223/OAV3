package com.oa.attendance.service;

import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysUser;

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
     * 根据ID查询用户
     */
    Result<SysUser> getUserById(Long id);

    /**
     * 更新用户信息
     */
    Result<?> updateUser(SysUser user);

    /**
     * 修改密码
     */
    Result<?> changePassword(Long userId, String oldPassword, String newPassword);
}