package com.oa.attendance.service.impl;

import com.oa.attendance.dto.DocumentCreateDTO;
import com.oa.attendance.dto.DocumentUpdateDTO;
import com.oa.attendance.entity.DocDocument;
import com.oa.attendance.entity.PolicyDocumentIndex;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.DocDocumentMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.repository.PolicyDocumentRepository;
import com.oa.attendance.service.IDocumentService;
import com.oa.attendance.vo.DocumentListVO;
import com.oa.attendance.vo.DocumentSearchVO;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 制度文档服务实现类
 */
@Service
public class DocumentServiceImpl implements IDocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    @Autowired
    private DocDocumentMapper documentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PolicyDocumentRepository documentRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    @Transactional
    public Result<?> create(DocumentCreateDTO dto, String currentUsername) {
        SysUser author = sysUserMapper.findByUsername(currentUsername);
        if (author == null) {
            return Result.error("当前登录用户不存在");
        }

        DocDocument document = new DocDocument();
        BeanUtils.copyProperties(dto, document);
        document.setAuthorId(author.getUserId());
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());

        int result = documentMapper.insert(document);
        if (result <= 0) {
            return Result.error("创建失败");
        }

        document.setAuthorName(author.getRealName());
        boolean synced = syncIndex(document);
        return Result.success(synced ? "创建成功" : "创建成功，ES索引同步失败，请检查Elasticsearch服务");
    }

    @Override
    @Transactional
    public Result<?> update(DocumentUpdateDTO dto) {
        DocDocument existing = documentMapper.selectById(dto.getDocId());
        if (existing == null) {
            return Result.error("文档不存在");
        }

        DocDocument document = new DocDocument();
        BeanUtils.copyProperties(dto, document);
        document.setUpdateTime(LocalDateTime.now());

        int result = documentMapper.updateById(document);
        if (result <= 0) {
            return Result.error("更新失败");
        }

        DocDocument detail = documentMapper.selectDocumentDetail(dto.getDocId());
        boolean synced = syncIndex(detail);
        return Result.success(synced ? "更新成功" : "更新成功，ES索引同步失败，请检查Elasticsearch服务");
    }

    @Override
    @Transactional
    public Result<?> delete(Long docId) {
        DocDocument existing = documentMapper.selectById(docId);
        if (existing == null) {
            return Result.error("文档不存在");
        }

        int result = documentMapper.deleteById(docId);
        if (result <= 0) {
            return Result.error("删除失败");
        }

        try {
            documentRepository.deleteById(docId);
        } catch (Exception e) {
            log.warn("删除ES文档索引失败: docId={}, error={}", docId, e.getMessage());
        }
        return Result.success("删除成功");
    }

    @Override
    public Result<DocumentListVO> getById(Long docId) {
        DocDocument document = documentMapper.selectDocumentDetail(docId);
        if (document == null) {
            return Result.error("文档不存在");
        }
        return Result.success("查询成功", buildListVO(document));
    }

    @Override
    public Result<List<DocumentListVO>> listAll() {
        List<DocumentListVO> documents = documentMapper.selectDocumentList()
                .stream()
                .map(this::buildListVO)
                .collect(Collectors.toList());
        return Result.success("查询成功", documents);
    }

    @Override
    public Result<List<DocumentSearchVO>> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Result.error("检索关键词不能为空");
        }

        try {
            NativeSearchQuery query = new NativeSearchQueryBuilder()
                    .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content")
                            .fuzziness(Fuzziness.AUTO))
                    .withHighlightFields(
                            new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                            new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>"))
                    .build();

            SearchHits<PolicyDocumentIndex> hits = elasticsearchRestTemplate.search(query, PolicyDocumentIndex.class);
            List<DocumentSearchVO> result = hits.stream()
                    .map(this::buildSearchVO)
                    .collect(Collectors.toList());
            return Result.success("检索成功", result);
        } catch (Exception e) {
            log.error("ES文档检索失败: keyword={}, error={}", keyword, e.getMessage(), e);
            return Result.error("Elasticsearch检索失败，请确认ES服务已启动并完成索引同步");
        }
    }

    @Override
    public Result<?> rebuildIndex() {
        try {
            List<PolicyDocumentIndex> indexes = documentMapper.selectDocumentList()
                    .stream()
                    .map(this::buildIndex)
                    .collect(Collectors.toList());

            documentRepository.deleteAll();
            documentRepository.saveAll(indexes);
            return Result.success("索引重建成功，共同步" + indexes.size() + "条文档");
        } catch (Exception e) {
            log.error("ES索引重建失败: {}", e.getMessage(), e);
            return Result.error("ES索引重建失败，请确认Elasticsearch服务已启动");
        }
    }

    private boolean syncIndex(DocDocument document) {
        try {
            documentRepository.save(buildIndex(document));
            return true;
        } catch (Exception e) {
            log.warn("同步ES文档索引失败: docId={}, error={}", document.getDocId(), e.getMessage());
            return false;
        }
    }

    private PolicyDocumentIndex buildIndex(DocDocument document) {
        PolicyDocumentIndex index = new PolicyDocumentIndex();
        index.setDocId(document.getDocId());
        index.setTitle(document.getTitle());
        index.setContent(document.getContent());
        index.setAuthorId(document.getAuthorId());
        index.setAuthorName(document.getAuthorName());
        index.setCreateTime(document.getCreateTime());
        index.setUpdateTime(document.getUpdateTime());
        return index;
    }

    private DocumentListVO buildListVO(DocDocument document) {
        DocumentListVO vo = new DocumentListVO();
        BeanUtils.copyProperties(document, vo);
        return vo;
    }

    private DocumentSearchVO buildSearchVO(SearchHit<PolicyDocumentIndex> hit) {
        PolicyDocumentIndex index = hit.getContent();
        DocumentSearchVO vo = new DocumentSearchVO();
        BeanUtils.copyProperties(index, vo);

        Map<String, List<String>> highlights = hit.getHighlightFields();
        List<String> titleHighlights = highlights.get("title");
        if (titleHighlights != null && !titleHighlights.isEmpty()) {
            vo.setTitleHighlight(titleHighlights.get(0));
        }
        List<String> contentHighlights = highlights.get("content");
        if (contentHighlights != null && !contentHighlights.isEmpty()) {
            vo.setContentHighlight(contentHighlights.get(0));
        }
        return vo;
    }
}
