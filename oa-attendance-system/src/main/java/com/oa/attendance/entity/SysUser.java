package com.oa.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_user")
public class SysUser {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;                 // 用户ID

    @TableField("employee_no")
    private String employeeNo;           // 工号

    @TableField("username")
    private String username;             // 登录账号

    @TableField("password")
    private String password;             // 登录密码

    @TableField("real_name")
    private String realName;             // 真实姓名

    @TableField("nickname")
    private String nickname;             // 昵称

    @TableField("avatar")
    private String avatar;               // 头像URL

    @TableField("gender")
    private String gender;               // 性别

    @TableField("phone")
    private String phone;                // 手机号

    @TableField("email")
    private String email;                // 邮箱

    @TableField("birth_date")
    private LocalDate birthDate;         // 生日

    @TableField("hire_date")
    private LocalDate hireDate;          // 入职日期

    @TableField("dept_id")
    private Long deptId;                 // 所属部门ID

    @TableField("position_id")
    private Long positionId;             // 职位ID

    @TableField("role_id")
    private Long roleId;                 // 角色ID

    @TableField("status")
    private Integer status;              // 状态：1正常 0禁用

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime; // 最后登录时间

    @TableField("create_time")
    private LocalDateTime createTime;    // 创建时间

    @TableField("update_time")
    private LocalDateTime updateTime;    // 更新时间

    // 临时字段，用于存储关联信息
    @TableField(exist = false)
    private String roleName;             // 角色名称

    @TableField(exist = false)
    private String deptName;             // 部门名称

    @TableField(exist = false)
    private String positionName;         // 职位名称
}