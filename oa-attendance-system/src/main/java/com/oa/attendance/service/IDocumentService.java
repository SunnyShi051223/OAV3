package com.oa.attendance.service;

import com.oa.attendance.dto.DocumentCreateDTO;
import com.oa.attendance.dto.DocumentUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.vo.DocumentListVO;
import com.oa.attendance.vo.DocumentSearchVO;

import java.util.List;

/**
 * 制度文档服务接口
 */
public interface IDocumentService {

    Result<?> create(DocumentCreateDTO dto, String currentUsername);

    Result<?> update(DocumentUpdateDTO dto);

    Result<?> delete(Long docId);

    Result<DocumentListVO> getById(Long docId);

    Result<List<DocumentListVO>> listAll();

    Result<List<DocumentSearchVO>> search(String keyword);

    Result<?> rebuildIndex();
}
