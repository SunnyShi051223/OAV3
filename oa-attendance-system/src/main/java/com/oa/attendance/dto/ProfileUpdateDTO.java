package com.oa.attendance.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 个人资料更新DTO
 */
@Data
public class ProfileUpdateDTO {

    private String nickname;
    private String gender;
    private String phone;
    private String email;
    private LocalDate birthDate;
}
