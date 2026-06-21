package com.trace.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 检索结果类
 */
@Data
@Builder
public class KnowledgeSearchResult {
    private List<Document> documents;
    private boolean fromWeb;
}
