package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 职位更新DTO
 */
@Data
public class PositionUpdateDTO {

    @NotNull(message = "职位ID不能为空")
    private Long positionId;             // 职位ID

    @NotBlank(message = "职位名称不能为空")
    private String positionName;         // 职位名称

    @NotNull(message = "部门ID不能为空")
    private Long deptId;                 // 所属部门ID

    private Integer level;               // 职位等级

    private String description;          // 职位描述
}