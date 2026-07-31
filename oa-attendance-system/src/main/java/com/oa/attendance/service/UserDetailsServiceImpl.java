package com.oa.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.entity.SysPermission;
import com.oa.attendance.entity.SysRole;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.SysPermissionMapper;
import com.oa.attendance.mapper.SysRoleMapper;
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

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 按登录账号查询用户
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        SysUser sysUser = sysUserMapper.selectOne(wrapper);

        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (sysUser.getDeleted() != null && sysUser.getDeleted() == 1) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 从数据库加载用户权限
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<SysPermission> permissions = sysPermissionMapper.selectPermissionsByUserId(sysUser.getUserId());
        if (permissions != null) {
            for (SysPermission perm : permissions) {
                authorities.add(new SimpleGrantedAuthority(perm.getPermissionCode()));
            }
        }
        if (sysUser.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(sysUser.getRoleId());
            if (role != null && role.getRoleCode() != null) {
                authorities.add(new SimpleGrantedAuthority(role.getRoleCode()));
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()));
            }
        }

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
