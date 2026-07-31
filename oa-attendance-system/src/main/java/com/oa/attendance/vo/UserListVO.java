package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户列表项VO
 */
@Data
public class UserListVO {

    private Long userId;                 // 用户ID
    private String employeeNo;           // 工号
    private String username;             // 登录账号
    private String realName;             // 真实姓名
    private String nickname;             // 昵称
    private String gender;               // 性别
    private String phone;                // 手机号
    private String email;                // 邮箱
    private LocalDate birthDate;         // 生日
    private LocalDate hireDate;          // 入职日期
    private Long deptId;                 // 所属部门ID
    private String deptName;             // 部门名称
    private Long positionId;             // 职位ID
    private String positionName;         // 职位名称
    private Long roleId;                 // 角色ID
    private String roleName;             // 角色名称
    private Integer status;              // 状态：1正常 0禁用
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间
}