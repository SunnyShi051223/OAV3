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
    @Select("SELECT d.*, u.real_name AS author_name " +
            "FROM doc_document d " +
            "LEFT JOIN sys_user u ON d.author_id = u.user_id " +
            "ORDER BY d.update_time DESC")
    List<DocDocument> selectDocumentList();

    /**
     * 根据ID查询文档，带作者名称
     */
    @Select("SELECT d.*, u.real_name AS author_name " +
            "FROM doc_document d " +
            "LEFT JOIN sys_user u ON d.author_id = u.user_id " +
            "WHERE d.doc_id = #{docId}")
    DocDocument selectDocumentDetail(@Param("docId") Long docId);
}
