package com.oa.attendance.vo;

import lombok.Data;

/**
 * 登录响应
 */
@Data
public class LoginVO {

    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private AuthUserVO user;
}
