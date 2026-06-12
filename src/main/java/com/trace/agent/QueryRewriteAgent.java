package com.trace.agent;

import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查询改写Agent —— 对模糊/口语化/缺少上下文的用户提示词进行改写，
 * 使其更适合向量检索和全文检索。
 */
@Slf4j
@Component
public class QueryRewriteAgent {

    private final ChatClient.Builder chatClientBuilder;
    private final MemoryService memoryService;

    public QueryRewriteAgent(ChatClient.Builder chatClientBuilder,
                             MemoryService memoryService) {
        this.chatClientBuilder = chatClientBuilder;
        this.memoryService = memoryService;
    }

    /**
     * 判断是否需要改写。
     *
     * @param query 用户原始查询
     * @return true 表示需要改写
     */
    public boolean needsRewrite(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String trimmed = query.trim();
        // 长度超过20字 → 通常足够清晰
        if (trimmed.length() > 20) {
            return false;
        }
        // 包含问句结构 → 不需要改写
        if (trimmed.contains("怎么")
                || trimmed.contains("如何")
                || trimmed.contains("是什么")
                || trimmed.contains("为什么")
                || trimmed.contains("区别")
                || trimmed.contains("原理")
                || trimmed.contains("怎样")
                || trimmed.contains("介绍")
                || trimmed.endsWith("？")
                || trimmed.endsWith("?")) {
            return false;
        }
        return true;
    }

    /**
     * 改写用户查询：太短/纯口语/代词指代不清 → 补充上下文后重新表述。
     *
     * @param userId  用户ID
     * @param query   原始查询
     * @param history 多轮对话历史（最近几轮用户消息）
     * @return 改写后的查询，或原始查询（如果无需改写）
     */
    public String rewrite(Long userId, String query, List<String> history) {
        if (!needsRewrite(query)) {
            return query;
        }
        try {
            StringBuilder ctx = new StringBuilder();
            if (history != null && !history.isEmpty()) {
                ctx.append("## 对话历史\n");
                for (int i = 0; i < history.size(); i++) {
                    ctx.append((i + 1)).append(". ").append(history.get(i)).append("\n");
                }
            }
            ChatClient chatClient = chatClientBuilder.build();
            String rewritten = chatClient.prompt()
                    .system("""
                            你是一个查询改写助手。用户的原始查询可能太短、口语化或包含不明确的指代词。
                            请根据对话历史，将用户的查询改写为一个完整、清晰的搜索查询。
                            规则：
                            1. 将代词（"他"、"它"、"这个"、"那个"等）替换为明确的实体名。
                            2. 将口语化表达转换为正式的搜索关键词。
                            3. 如果查询已经足够清晰，直接返回原文。
                            4. 只输出改写后的查询，不要输出任何解释。
                            """)
                    .user(ctx + "\n原始查询：" + query)
                    .call()
                    .content();
            log.info("Query rewritten: '{}' → '{}'", query, rewritten);
            return rewritten != null && !rewritten.isBlank() ? rewritten : query;
        } catch (Exception e) {
            log.warn("Query rewrite failed, using original: {}", query, e);
            return query;
        }
    }

    /**
     * 获取用户最近的对话历史（用于上下文改写）。
     */
    public List<String> getRecentHistory(Long userId) {
        List<Map<String, String>> ctx = memoryService.getChatContext(userId);
        if (ctx == null || ctx.isEmpty()) {
            return List.of();
        }
        return ctx.stream()
                .filter(m -> "user".equals(m.get("role")))
                .map(m -> m.get("content"))
                .toList();
    }
}
