package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.dto.UserCreateDTO;
import com.oa.attendance.dto.UserUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysDepartment;
import com.oa.attendance.entity.SysPermission;
import com.oa.attendance.entity.SysPosition;
import com.oa.attendance.entity.SysRole;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.exception.BusinessException;
import com.oa.attendance.mapper.SysDepartmentMapper;
import com.oa.attendance.mapper.SysPermissionMapper;
import com.oa.attendance.mapper.SysPositionMapper;
import com.oa.attendance.mapper.SysRoleMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.IUserService;
import com.oa.attendance.service.TokenBlacklistService;
import com.oa.attendance.util.JwtUtil;
import com.oa.attendance.vo.AuthUserVO;
import com.oa.attendance.vo.LoginVO;
import com.oa.attendance.vo.UserListVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements IUserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysDepartmentMapper sysDepartmentMapper;

    @Autowired
    private SysPositionMapper sysPositionMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    public Result<?> login(String username, String password) {
        try {
            // 使用Spring Security进行认证
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));

            // 获取认证后的用户信息
            org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            // 根据用户名查询用户详细信息以获取用户ID
            QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
            wrapper.eq("username", userDetails.getUsername()).eq("deleted", 0);
            SysUser user = sysUserMapper.selectOne(wrapper);

            if (user == null) {
                return Result.error("用户不存在");
            }

            user.setLastLoginTime(LocalDateTime.now());
            sysUserMapper.updateById(user);

            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getUserId());
            LoginVO loginVO = new LoginVO();
            loginVO.setToken(token);
            loginVO.setExpiresIn(jwtUtil.getRemainingSeconds(token));
            loginVO.setUser(buildAuthUser(user));

            // 返回用户信息和Token
            return Result.success("登录成功", loginVO);
        } catch (Exception e) {
            log.error("登录失败: username={}, error={}", username, e.getMessage());
            return Result.error("用户名或密码错误");
        }
    }

    @Override
    public Result<?> logout(String token) {
        tokenBlacklistService.blacklist(token);
        SecurityContextHolder.clearContext();
        return Result.success("退出成功");
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
            wrapper.and(w -> w.eq("username", username).or().eq("employee_no", username));
            wrapper.eq("deleted", 0);
            SysUser user = sysUserMapper.selectOne(wrapper);

            if (user != null) {
                return Result.success("获取成功", buildAuthUser(user));
            }
        }

        return Result.error("未登录或登录已失效");
    }

    private AuthUserVO buildAuthUser(SysUser user) {
        AuthUserVO authUserVO = new AuthUserVO();
        BeanUtils.copyProperties(user, authUserVO);

        if (user.getDeptId() != null) {
            SysDepartment dept = sysDepartmentMapper.selectById(user.getDeptId());
            if (dept != null) {
                authUserVO.setDeptName(dept.getDeptName());
            }
        }

        if (user.getPositionId() != null) {
            SysPosition position = sysPositionMapper.selectById(user.getPositionId());
            if (position != null) {
                authUserVO.setPositionName(position.getPositionName());
            }
        }

        if (user.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(user.getRoleId());
            if (role != null) {
                authUserVO.setRoleName(role.getRoleName());
                authUserVO.setRoleCode(role.getRoleCode());
            }
        }

        List<SysPermission> permissions = sysPermissionMapper.selectPermissionsByUserId(user.getUserId());
        if (permissions != null) {
            authUserVO.setPermissions(permissions.stream()
                    .map(SysPermission::getPermissionCode)
                    .collect(Collectors.toList()));
        }

        return authUserVO;
    }

    @Override
    @Transactional
    public Result<?> create(UserCreateDTO dto) {
        // 检查工号是否已存在
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_no", dto.getEmployeeNo()).eq("deleted", 0);
        Long count = sysUserMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("工号已存在");
        }

        // 检查用户名是否已存在
        wrapper = new QueryWrapper<>();
        wrapper.eq("username", dto.getUsername()).eq("deleted", 0);
        count = sysUserMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("用户名已存在");
        }

        // 检查关联数据是否存在
        if (dto.getDeptId() != null) {
            SysDepartment dept = sysDepartmentMapper.selectById(dto.getDeptId());
            if (dept == null) {
                return Result.error("部门不存在");
            }
        }

        if (dto.getPositionId() != null) {
            SysPosition position = sysPositionMapper.selectById(dto.getPositionId());
            if (position == null) {
                return Result.error("职位不存在");
            }
        }

        if (dto.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(dto.getRoleId());
            if (role == null) {
                return Result.error("角色不存在");
            }
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // 加密密码
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        int result = sysUserMapper.insert(user);
        if (result > 0) {
            return Result.success("创建成功");
        }
        return Result.error("创建失败");
    }

    @Override
    @Transactional
    public Result<?> update(UserUpdateDTO dto) {
        SysUser existing = sysUserMapper.selectById(dto.getUserId());
        if (existing == null) {
            return Result.error("用户不存在");
        }

        // 检查关联数据是否存在
        if (dto.getDeptId() != null) {
            SysDepartment dept = sysDepartmentMapper.selectById(dto.getDeptId());
            if (dept == null) {
                return Result.error("部门不存在");
            }
        }

        if (dto.getPositionId() != null) {
            SysPosition position = sysPositionMapper.selectById(dto.getPositionId());
            if (position == null) {
                return Result.error("职位不存在");
            }
        }

        if (dto.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(dto.getRoleId());
            if (role == null) {
                return Result.error("角色不存在");
            }
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setUpdateTime(LocalDateTime.now());

        int result = sysUserMapper.updateById(user);
        if (result > 0) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    @Override
    @Transactional
    public Result<?> delete(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        int result = sysUserMapper.softDeleteById(userId);
        if (result > 0) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    @Override
    public Result<UserListVO> getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user != null && user.getDeleted() == 0) {
            UserListVO vo = new UserListVO();
            BeanUtils.copyProperties(user, vo);

            // 设置部门名称
            if (user.getDeptId() != null) {
                SysDepartment dept = sysDepartmentMapper.selectById(user.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getDeptName());
                }
            }

            // 设置职位名称
            if (user.getPositionId() != null) {
                SysPosition position = sysPositionMapper.selectById(user.getPositionId());
                if (position != null) {
                    vo.setPositionName(position.getPositionName());
                }
            }

            // 设置角色名称
            if (user.getRoleId() != null) {
                SysRole role = sysRoleMapper.selectById(user.getRoleId());
                if (role != null) {
                    vo.setRoleName(role.getRoleName());
                }
            }

            return Result.success("查询成功", vo);
        }
        return Result.error("用户不存在");
    }

    @Override
    public Result<List<UserListVO>> listAll() {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        wrapper.orderByDesc("create_time");

        List<SysUser> users = sysUserMapper.selectList(wrapper);
        List<UserListVO> vos = users.stream().map(user -> {
            UserListVO vo = new UserListVO();
            BeanUtils.copyProperties(user, vo);

            // 设置部门名称
            if (user.getDeptId() != null) {
                SysDepartment dept = sysDepartmentMapper.selectById(user.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getDeptName());
                }
            }

            // 设置职位名称
            if (user.getPositionId() != null) {
                SysPosition position = sysPositionMapper.selectById(user.getPositionId());
                if (position != null) {
                    vo.setPositionName(position.getPositionName());
                }
            }

            // 设置角色名称
            if (user.getRoleId() != null) {
                SysRole role = sysRoleMapper.selectById(user.getRoleId());
                if (role != null) {
                    vo.setRoleName(role.getRoleName());
                }
            }

            return vo;
        }).collect(Collectors.toList());

        return Result.success("查询成功", vos);
    }

    @Override
    public Result<List<UserListVO>> listByDeptId(Long deptId) {
        SysDepartment dept = sysDepartmentMapper.selectById(deptId);
        if (dept == null) {
            return Result.error("部门不存在");
        }

        List<SysUser> users = sysUserMapper.selectByDeptId(deptId);
        List<UserListVO> vos = users.stream().map(user -> {
            UserListVO vo = new UserListVO();
            BeanUtils.copyProperties(user, vo);

            // 部门名称已经在查询中获取了
            vo.setDeptName(dept.getDeptName());

            return vo;
        }).collect(Collectors.toList());

        return Result.success("查询成功", vos);
    }

    @Override
    public Result<?> updateUserById(UserUpdateDTO dto) {
        SysUser existing = sysUserMapper.selectById(dto.getUserId());
        if (existing == null) {
            return Result.error("用户不存在");
        }

        // 检查关联数据是否存在
        if (dto.getDeptId() != null) {
            SysDepartment dept = sysDepartmentMapper.selectById(dto.getDeptId());
            if (dept == null) {
                return Result.error("部门不存在");
            }
        }

        if (dto.getPositionId() != null) {
            SysPosition position = sysPositionMapper.selectById(dto.getPositionId());
            if (position == null) {
                return Result.error("职位不存在");
            }
        }

        if (dto.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(dto.getRoleId());
            if (role == null) {
                return Result.error("角色不存在");
            }
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setUpdateTime(LocalDateTime.now());

        int result = sysUserMapper.updateById(user);
        if (result > 0) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    @Override
    public Result<?> changePassword(Long userId, String oldPassword, String newPassword, String currentUsername) {
        // 查询用户
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 只能修改自己的密码
        if (!user.getUsername().equals(currentUsername)) {
            return Result.error("只能修改自己的密码");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        int result = sysUserMapper.updateById(user);
        if (result > 0) {
            return Result.success("密码修改成功");
        }
        return Result.error("密码修改失败");
    }
}
