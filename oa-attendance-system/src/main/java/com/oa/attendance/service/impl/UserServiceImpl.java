package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oa.attendance.dto.UserCreateDTO;
import com.oa.attendance.dto.UserUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysDepartment;
import com.oa.attendance.entity.SysPosition;
import com.oa.attendance.entity.SysRole;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.exception.BusinessException;
import com.oa.attendance.mapper.SysDepartmentMapper;
import com.oa.attendance.mapper.SysPositionMapper;
import com.oa.attendance.mapper.SysRoleMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.IUserService;
import com.oa.attendance.util.JwtUtil;
import com.oa.attendance.vo.UserListVO;
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

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysDepartmentMapper sysDepartmentMapper;

    @Autowired
    private SysPositionMapper sysPositionMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

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
            LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
            wrapper.and(w -> w.eq(SysUser::getUsername, username).or().eq(SysUser::getEmployeeNo, username))
                   .eq(SysUser::getDeleted, 0);
            SysUser user = sysUserMapper.selectOne(wrapper);

            if (user != null) {
                // 查询关联信息
                UserListVO userVo = new UserListVO();
                BeanUtils.copyProperties(user, userVo);

                // 设置部门名称
                if (user.getDeptId() != null) {
                    SysDepartment dept = sysDepartmentMapper.selectById(user.getDeptId());
                    if (dept != null) {
                        userVo.setDeptName(dept.getDeptName());
                    }
                }

                // 设置职位名称
                if (user.getPositionId() != null) {
                    SysPosition position = sysPositionMapper.selectById(user.getPositionId());
                    if (position != null) {
                        userVo.setPositionName(position.getPositionName());
                    }
                }

                // 设置角色名称
                if (user.getRoleId() != null) {
                    SysRole role = sysRoleMapper.selectById(user.getRoleId());
                    if (role != null) {
                        userVo.setRoleName(role.getRoleName());
                    }
                }

                return Result.success("获取成功", userVo);
            }
        }

        return Result.error("未登录或登录已失效");
    }

    @Override
    @Transactional
    public Result<?> create(UserCreateDTO dto) {
        // 检查工号是否已存在
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getEmployeeNo, dto.getEmployeeNo()).eq(SysUser::getDeleted, 0);
        int count = sysUserMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("工号已存在");
        }

        // 检查用户名是否已存在
        wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUsername, dto.getUsername()).eq(SysUser::getDeleted, 0);
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

        user.setDeleted(1);
        user.setUpdateTime(LocalDateTime.now());

        int result = sysUserMapper.updateById(user);
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
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getDeleted, 0).orderByDesc(SysUser::getCreateTime);

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
        user.setUpdateTime(LocalDateTime.now());
        int result = sysUserMapper.updateById(user);
        if (result > 0) {
            return Result.success("密码修改成功");
        }
        return Result.error("密码修改失败");
    }
}