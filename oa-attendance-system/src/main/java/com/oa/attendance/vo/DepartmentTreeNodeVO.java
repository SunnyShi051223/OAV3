package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门树形节点VO
 */
@Data
public class DepartmentTreeNodeVO {

    private Long deptId;                 // 部门ID
    private Long parentId;               // 父级部门ID
    private String deptName;             // 部门名称
    private String deptCode;             // 部门代码
    private String leaderName;           // 部门负责人名称
    private String description;          // 部门描述
    private Integer status;              // 状态：1启用 0禁用
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间

    private List<DepartmentTreeNodeVO> children;  // 子部门列表
}