package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ColleagueVO {

    private Long userId;
    private String employeeNo;
    private String realName;
    private String avatar;
    private String gender;
    private String phone;
    private String email;
    private LocalDate hireDate;
    private Long deptId;
    private String deptName;
    private Long positionId;
    private String positionName;
    private String roleName;
}
