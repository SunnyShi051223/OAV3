package com.oa.attendance.repository;

import com.oa.attendance.entity.PolicyDocumentIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 制度文档ES仓储
 */
@Repository
public interface PolicyDocumentRepository extends ElasticsearchRepository<PolicyDocumentIndex, Long> {
}
