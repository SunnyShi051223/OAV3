package com.oa.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 制度文档实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("doc_document")
public class DocDocument {

    @TableId(value = "doc_id", type = IdType.AUTO)
    private Long docId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("author_id")
    private Long authorId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String authorName;

    @TableField(exist = false)
    private String tags;
}
