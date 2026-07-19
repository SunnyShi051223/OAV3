package com.oa.attendance.controller;

import com.oa.attendance.dto.UserCreateDTO;
import com.oa.attendance.dto.UserUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IUserService;
import com.oa.attendance.vo.UserListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
@Validated
@CrossOrigin
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasAuthority('user:add')")
    public Result<?> create(@Valid @RequestBody UserCreateDTO dto) {
        return userService.create(dto);
    }

    /**
     * 更新用户
     */
    @PutMapping
    @PreAuthorize("hasAuthority('user:update')")
    public Result<?> update(@Valid @RequestBody UserUpdateDTO dto) {
        return userService.update(dto);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public Result<?> delete(@PathVariable Long id) {
        return userService.delete(id);
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:query')")
    public Result<UserListVO> getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    /**
     * 查询所有用户
     */
    @GetMapping
    @PreAuthorize("hasAuthority('user:query')")
    public Result<List<UserListVO>> listAll() {
        return userService.listAll();
    }

    /**
     * 根据部门ID查询用户列表
     */
    @GetMapping("/dept/{deptId}")
    @PreAuthorize("hasAuthority('user:query')")
    public Result<List<UserListVO>> listByDeptId(@PathVariable Long deptId) {
        return userService.listByDeptId(deptId);
    }

    /**
     * 修改密码
     */
    @PutMapping("/{userId}/password")
    public Result<?> changePassword(@PathVariable Long userId,
                                   @RequestParam String oldPassword,
                                   @RequestParam String newPassword) {
        return userService.changePassword(userId, oldPassword, newPassword);
    }
}