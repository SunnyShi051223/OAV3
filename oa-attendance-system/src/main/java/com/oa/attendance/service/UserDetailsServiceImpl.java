package com.oa.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户详情服务类
 * 实现Spring Security的UserDetailsService接口
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 尝试将输入转换为数字，判断是否是用户ID
        Long userId = null;
        try {
            userId = Long.parseLong(username);
        } catch (NumberFormatException e) {
            // 不是数字，按用户名/工号查询
        }

        SysUser sysUser;
        if (userId != null) {
            // 按ID查询用户
            sysUser = sysUserMapper.selectById(userId);
        } else {
            // 按用户名/工号查询用户
            QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
            wrapper.eq("username", username).or().eq("employee_no", username);

            sysUser = sysUserMapper.selectOne(wrapper);
        }

        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 获取用户权限列表
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 这里可以根据用户的角色或权限表来设置权限
        // 为简单起见，暂时给每个用户赋予USER权限
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // 构建UserDetails对象
        return User.builder()
                .username(sysUser.getUsername())
                .password(sysUser.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(sysUser.getStatus() == 0) // 0为禁用，1为正常
                .build();
    }
}