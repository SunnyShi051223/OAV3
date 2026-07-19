package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.IUserService;
import com.oa.attendance.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Result<?> login(String username, String password) {
        try {
            // 使用Spring Security进行认证
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));

            // 获取用户信息
            SysUser user = (SysUser) authentication.getPrincipal();

            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getUserId());

            // 返回用户信息和Token
            return Result.success("登录成功", token);
        } catch (Exception e) {
            return Result.error("用户名或密码错误");
        }
    }

    @Override
    public Result<?> getCurrentUserInfo() {
        // 从SecurityContext获取当前认证用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {

            // 从Token中获取用户ID并查询详细信息
            String username = authentication.getName();
            QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
            wrapper.eq("username", username).or().eq("employee_no", username);
            SysUser user = sysUserMapper.selectOne(wrapper);

            return Result.success("获取成功", user);
        }

        return Result.error("未登录或登录已失效");
    }

    @Override
    public Result<SysUser> getUserById(Long id) {
        SysUser user = sysUserMapper.getUserDetailById(id);
        if (user != null) {
            return Result.success("查询成功", user);
        }
        return Result.error("用户不存在");
    }

    @Override
    public Result<?> updateUser(SysUser user) {
        int result = sysUserMapper.updateById(user);
        if (result > 0) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    @Override
    public Result<?> changePassword(Long userId, String oldPassword, String newPassword) {
        // 查询用户
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        int result = sysUserMapper.updateById(user);
        if (result > 0) {
            return Result.success("密码修改成功");
        }
        return Result.error("密码修改失败");
    }
}