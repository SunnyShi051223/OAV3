package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 用户创建DTO
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "工号不能为空")
    @Size(max = 50, message = "工号不能超过50个字符")
    private String employeeNo;           // 工号

    @NotBlank(message = "登录账号不能为空")
    @Size(max = 50, message = "登录账号不能超过50个字符")
    private String username;             // 登录账号

    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, max = 64, message = "登录密码长度必须为6到64个字符")
    private String password;             // 登录密码

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名不能超过50个字符")
    private String realName;             // 真实姓名

    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;             // 昵称

    @Size(max = 10, message = "性别不能超过10个字符")
    private String gender;               // 性别

    @Size(max = 20, message = "手机号不能超过20个字符")
    private String phone;                // 手机号

    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;                // 邮箱

    private LocalDate birthDate;         // 生日

    private LocalDate hireDate;          // 入职日期

    @NotNull(message = "部门ID不能为空")
    private Long deptId;                 // 所属部门ID

    private Long positionId;             // 职位ID

    @NotNull(message = "角色ID不能为空")
    private Long roleId;                 // 角色ID

    @Min(value = 0, message = "用户状态不正确")
    @Max(value = 1, message = "用户状态不正确")
    private Integer status;              // 状态：1正常 0禁用
}
