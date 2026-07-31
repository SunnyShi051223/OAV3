package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 职位列表项VO
 */
@Data
public class PositionListVO {

    private Long positionId;             // 职位ID
    private String positionName;         // 职位名称
    private Long deptId;                 // 所属部门ID
    private String deptName;             // 部门名称
    private Integer level;               // 职位等级
    private String description;          // 职位描述
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间
}