package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApplicationCreateDTO {

    @NotBlank(message = "申请类型不能为空")
    @Size(max = 50, message = "申请类型不能超过50个字符")
    private String applicationType;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotBlank(message = "申请原因不能为空")
    @Size(max = 1000, message = "申请原因不能超过1000个字符")
    private String reason;

    @Size(max = 10, message = "申请附件最多10个")
    private List<String> attachmentUrls;

    @Size(max = 1000, message = "申请备注不能超过1000个字符")
    private String remark;
}
