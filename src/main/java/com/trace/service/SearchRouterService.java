package com.trace.service;

import com.trace.entity.KnowledgeBase;
import com.trace.enums.SearchType;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * 统一检索路由服务 —— 根据检索类型和场景选择检索策略。
 */
public interface SearchRouterService {

    /**
     * 执行检索。
     *
     * @param userId       用户ID
     * @param query        查询文本
     * @param searchType   检索类型
     * @param knowledgeType 知识库类型（USER/INTERVIEW/WEB/ALL），空表示全部
     * @param topK         返回条数
     * @return 检索结果
     */
    List<Document> search(Long userId,
                          String query,
                          SearchType searchType,
                          String knowledgeType,
                          int topK);

    /**
     * 知识问答三级检索：知识库优先 → 无结果时网络兜底 → 知识类结果存入WEB库。
     *
     * @param userId      用户ID
     * @param query       查询文本
     * @param forceWeb    用户是否明确要求联网
     * @return 检索结果 + 是否来自网络
     */
    Map<String, Object> knowledgeSearch(Long userId, String query, boolean forceWeb);

    /**
     * 模拟面试检索：仅查INTERVIEW知识库，混合检索。
     */
    List<Document> interviewSearch(Long userId, String query, int topK);
}
