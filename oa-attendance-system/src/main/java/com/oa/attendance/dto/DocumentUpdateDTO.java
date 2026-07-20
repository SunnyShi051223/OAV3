package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 制度文档更新DTO
 */
@Data
public class DocumentUpdateDTO {

    @NotNull(message = "文档ID不能为空")
    private Long docId;

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题不能超过200个字符")
    private String title;

    @NotBlank(message = "文档内容不能为空")
    private String content;
}
