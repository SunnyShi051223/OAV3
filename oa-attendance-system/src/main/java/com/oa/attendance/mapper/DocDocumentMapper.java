package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.DocDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 制度文档Mapper接口
 */
@Mapper
public interface DocDocumentMapper extends BaseMapper<DocDocument> {

    /**
     * 查询文档列表，带作者名称
     */
    @Select("SELECT d.*, u.real_name AS author_name, tag_rel.tags " +
            "FROM doc_document d " +
            "LEFT JOIN sys_user u ON d.author_id = u.user_id " +
            "LEFT JOIN (" +
            "  SELECT dt.doc_id, GROUP_CONCAT(t.tag_name ORDER BY t.tag_id SEPARATOR ',') AS tags " +
            "  FROM doc_document_tag dt " +
            "  INNER JOIN doc_tag t ON dt.tag_id = t.tag_id AND t.status = 1 " +
            "  GROUP BY dt.doc_id" +
            ") tag_rel ON d.doc_id = tag_rel.doc_id " +
            "ORDER BY d.update_time DESC")
    List<DocDocument> selectDocumentList();

    /**
     * 根据ID查询文档，带作者名称
     */
    @Select("SELECT d.*, u.real_name AS author_name, tag_rel.tags " +
            "FROM doc_document d " +
            "LEFT JOIN sys_user u ON d.author_id = u.user_id " +
            "LEFT JOIN (" +
            "  SELECT dt.doc_id, GROUP_CONCAT(t.tag_name ORDER BY t.tag_id SEPARATOR ',') AS tags " +
            "  FROM doc_document_tag dt " +
            "  INNER JOIN doc_tag t ON dt.tag_id = t.tag_id AND t.status = 1 " +
            "  GROUP BY dt.doc_id" +
            ") tag_rel ON d.doc_id = tag_rel.doc_id " +
            "WHERE d.doc_id = #{docId}")
    DocDocument selectDocumentDetail(@Param("docId") Long docId);
}
