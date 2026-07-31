package com.oa.attendance.controller;

import com.oa.attendance.dto.LoginDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

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
    public Result<?> login(@Valid @RequestBody LoginDTO dto) {
        return userService.login(dto.getUsername(), dto.getPassword());
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
    public Result<?> logout(HttpServletRequest request) {
        return userService.logout(parseToken(request));
    }

    private String parseToken(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
