package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 制度文档创建DTO
 */
@Data
public class DocumentCreateDTO {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题不能超过200个字符")
    private String title;

    @NotBlank(message = "文档内容不能为空")
    private String content;
}
