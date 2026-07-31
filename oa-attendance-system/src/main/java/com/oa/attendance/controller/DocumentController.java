package com.oa.attendance.controller;

import com.oa.attendance.dto.DocumentCreateDTO;
import com.oa.attendance.dto.DocumentUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IDocumentService;
import com.oa.attendance.vo.DocumentListVO;
import com.oa.attendance.vo.DocumentSearchVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 制度文档控制器
 */
@RestController
@RequestMapping("/api/documents")
@Validated
@CrossOrigin
public class DocumentController {

    @Autowired
    private IDocumentService documentService;

    @PostMapping
    @PreAuthorize("hasAuthority('document:add')")
    public Result<?> create(@Valid @RequestBody DocumentCreateDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return documentService.create(dto, auth.getName());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('document:update')")
    public Result<?> update(@Valid @RequestBody DocumentUpdateDTO dto) {
        return documentService.update(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('document:delete')")
    public Result<?> delete(@PathVariable Long id) {
        return documentService.delete(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('document:query')")
    public Result<DocumentListVO> getById(@PathVariable Long id) {
        return documentService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('document:query')")
    public Result<List<DocumentListVO>> listAll() {
        return documentService.listAll();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('document:query')")
    public Result<List<DocumentSearchVO>> search(@RequestParam String keyword) {
        return documentService.search(keyword);
    }

    @PostMapping("/index/rebuild")
    @PreAuthorize("hasAuthority('document:index')")
    public Result<?> rebuildIndex() {
        return documentService.rebuildIndex();
    }
}
