package com.oa.attendance.controller;

import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private IUserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<?> login(@RequestParam @NotBlank(message = "用户名不能为空") String username,
                           @RequestParam @NotBlank(message = "密码不能为空") String password) {
        return userService.login(username, password);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<?> getCurrentUserInfo() {
        return userService.getCurrentUserInfo();
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        // 在JWT模式下，服务端无需特殊处理退出，只需前端清除Token即可
        return Result.success("退出成功");
    }
}