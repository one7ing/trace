package com.trace.agent;

import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 查询改写Agent —— 对模糊/口语化/缺少上下文的用户提示词进行改写，
 * 使其更适合向量检索和全文检索。
 */
@Slf4j
@Component
public class QueryRewriteAgent {
    private String cachedPrompt;
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
        return !trimmed.contains("怎么")
                && !trimmed.contains("如何")
                && !trimmed.contains("是什么")
                && !trimmed.contains("为什么")
                && !trimmed.contains("区别")
                && !trimmed.contains("原理")
                && !trimmed.contains("怎样")
                && !trimmed.contains("介绍")
                && !trimmed.endsWith("？")
                && !trimmed.endsWith("?");
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
            String rewritten = chatClient
                    .prompt()
                    .system(systemprompt())
                    .user(ctx + "\n原始查询：" + query)
                    .call()
                    .content();
            log.info("查询改写完成: '{}' → '{}'", query, rewritten);
            return rewritten != null && !rewritten.isBlank() ? rewritten : query;
        } catch (Exception e) {
            log.warn("查询改写失败，使用原始查询: {}", query, e);
            return query;
        }
    }
    /**
     * 构建系统提示词
     */
    public String systemprompt(){
        if(cachedPrompt==null) {
            try {
                cachedPrompt = new ClassPathResource("agent/prompts/Rewrite")
                        .getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "你要重写用户提示词，使其清晰";
            }
        }
        return cachedPrompt;
    }
}
