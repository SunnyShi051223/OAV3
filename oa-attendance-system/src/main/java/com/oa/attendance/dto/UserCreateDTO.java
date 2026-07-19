package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 用户创建DTO
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "工号不能为空")
    private String employeeNo;           // 工号

    @NotBlank(message = "登录账号不能为空")
    private String username;             // 登录账号

    @NotBlank(message = "登录密码不能为空")
    private String password;             // 登录密码

    @NotBlank(message = "真实姓名不能为空")
    private String realName;             // 真实姓名

    private String nickname;             // 昵称

    private String gender;               // 性别

    private String phone;                // 手机号

    private String email;                // 邮箱

    private LocalDate birthDate;         // 生日

    private LocalDate hireDate;          // 入职日期

    @NotNull(message = "部门ID不能为空")
    private Long deptId;                 // 所属部门ID

    private Long positionId;             // 职位ID

    @NotNull(message = "角色ID不能为空")
    private Long roleId;                 // 角色ID

    private Integer status;              // 状态：1正常 0禁用
}