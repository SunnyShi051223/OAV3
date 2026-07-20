package com.oa.attendance.vo;

import lombok.Data;

/**
 * 制度文档检索VO
 */
@Data
public class DocumentSearchVO extends DocumentListVO {

    private String titleHighlight;
    private String contentHighlight;
}
