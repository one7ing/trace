package com.trace.service.impl;

import com.trace.entity.KnowledgeBase;
import com.trace.enums.SearchType;
import com.trace.service.KnowledgeBaseService;
import com.trace.service.SearchRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一检索路由实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchRouterServiceImpl implements SearchRouterService {

    private final KnowledgeBaseService kbService;

    /** 时间性关键词——不适合存入WEB知识库 */
    private static final Set<String> TIME_KEYWORDS = Set.of(
            "今天", "明天", "昨天", "现在", "几点", "几号", "星期",
            "天气", "温度", "股价", "汇率", "最新", "刚刚", "当前"
    );

    @Override
    public List<Document> search(Long userId,
                                 String query,
                                 SearchType searchType,
                                 String knowledgeType,
                                 int topK) {
        switch (searchType) {
            case HYBRID:
                return kbService.hybridSearch(userId, query, knowledgeType, topK);
            case SEMANTIC: {
                // 已废弃：语义检索请使用 KnowledgeBaseService.semanticSearch()
                return kbService.semanticSearch(userId, query, knowledgeType)
                        .stream().collect(Collectors.toList());
            }
            case KEYWORD:
                return keywordSearch(userId, query, knowledgeType, topK);
            default:
                return List.of();
        }
    }

    @Override
    public Map<String, Object> knowledgeSearch(Long userId,
                                                String query,
                                                boolean forceWeb) {
        Map<String, Object> result = new HashMap<>();
        result.put("fromWeb", false);

        if (forceWeb) {
            result.put("fromWeb", true);
            result.put("results", List.of());
            return result;
        }

        // 先查知识库（USER + INTERVIEW + WEB）
        List<Document> docs = hybridSearch(userId, query, "ALL", 5);
        if (!docs.isEmpty()) {
            result.put("results", docs);
            return result;
        }

        // 无结果 → 网络检索
        result.put("fromWeb", true);
        result.put("results", docs);

        // 判断是否为知识类内容（非时间性）
        if (isKnowledgeContent(query)) {
            saveToWebLibrary(userId, query);
        }

        return result;
    }

    @Override
    public List<Document> interviewSearch(Long userId, String query, int topK) {
        return hybridSearch(userId, query, "INTERVIEW", topK);
    }

    /**
     * 混合检索。
     */
    private List<Document> hybridSearch(Long userId,
                                        String query,
                                        String knowledgeType,
                                        int topK) {
        return kbService.hybridSearch(userId, query,
                "ALL".equals(knowledgeType) ? "" : knowledgeType, topK);
    }

    /**
     * 关键词全文检索——使用 PostgreSQL tsquery。
     */
    private List<Document> keywordSearch(Long userId,
                                         String query,
                                         String knowledgeType,
                                         int topK) {
        // 委托给混合检索（其中全文权重更高）
        return kbService.hybridSearch(userId, query,
                "ALL".equals(knowledgeType) ? "" : knowledgeType, topK);
    }

    /**
     * 判断内容是否为知识类（非时间敏感）。
     */
    private boolean isKnowledgeContent(String query) {
        for (String kw : TIME_KEYWORDS) {
            if (query.contains(kw)) {
                return false;
            }
        }
        // 问句结构 → 知识类
        return query.contains("怎么")
                || query.contains("如何")
                || query.contains("是什么")
                || query.contains("为什么")
                || query.contains("原理")
                || query.contains("区别")
                || query.contains("介绍")
                || query.endsWith("？")
                || query.endsWith("?");
    }

    /**
     * 将知识类搜索存入WEB知识库（待后续自动入库）。
     */
    private void saveToWebLibrary(Long userId, String query) {
        log.info("知识查询符合WEB知识库入库条件: userId={}, query={}", userId, query);
         //TODO: 后续实现自动入库（需接入网络搜索结果回调）
    }
}
