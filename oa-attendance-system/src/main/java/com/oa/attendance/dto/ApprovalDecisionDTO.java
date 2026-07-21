package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class ApprovalDecisionDTO {

    @Size(max = 500, message = "审批意见不能超过500个字符")
    private String comment;
}
