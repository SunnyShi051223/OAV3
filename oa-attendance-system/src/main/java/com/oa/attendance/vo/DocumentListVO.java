package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 制度文档列表VO
 */
@Data
public class DocumentListVO {

    private Long docId;
    private String title;
    private String content;
    private Long authorId;
    private String authorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
