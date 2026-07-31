package com.oa.attendance.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息
 */
@Data
public class AuthUserVO {

    private Long userId;
    private String employeeNo;
    private String username;
    private String realName;
    private String nickname;
    private String avatar;
    private Long deptId;
    private String deptName;
    private Long positionId;
    private String positionName;
    private Long roleId;
    private String roleName;
    private String roleCode;
    private List<String> permissions;
}
