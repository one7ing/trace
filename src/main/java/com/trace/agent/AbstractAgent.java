package com.trace.agent;

import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent 抽象基类 —— 封装公共逻辑（记忆检索、上下文构建、日志）
 * 子类只需实现 name() 和 buildSystemPrompt()
 */
@Slf4j
public abstract class AbstractAgent implements Agent {

    protected final ChatClient.Builder chatClientBuilder;
    protected final MemoryService memoryService;

    /** 全局取消令牌 —— key: userId, value: 是否取消 */
    public static final ConcurrentMap<Long, AtomicBoolean> CANCEL_MAP = new ConcurrentHashMap<>();

    protected AbstractAgent(ChatClient.Builder chatClientBuilder,
                             MemoryService memoryService) {
        this.chatClientBuilder = chatClientBuilder;
        this.memoryService = memoryService;
    }

    /** 加载此 Agent 的 System Prompt（子类实现） */
    protected abstract String loadSystemPrompt();

    /** 默认匹配：关键词检测 */
    protected boolean matchKeywords(String input, String... keywords) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) return true;
        }
        return false;
    }

    protected List<com.trace.entity.LongTermMemory> searchMemory(Long userId, String query, int limit) {
        return memoryService.getRecentMemories(userId, limit);
    }

    /** 构建对话上下文 */
    protected String buildContext(Long userId, String userInput) {
        var memories = searchMemory(userId, userInput, 5);
        StringBuilder sb = new StringBuilder();
        if (!memories.isEmpty()) {
            sb.append("## 用户历史相关记忆：\n");
            for (var m : memories) {
                sb.append("- ").append(m.getContent()).append("\n");
            }
        }
        return sb.toString();
    }

    /** 公共流式处理 */
    @Override
    public reactor.core.publisher.Flux<String> handleStream(String userInput, Long userId) {
        AtomicBoolean cancelled = CANCEL_MAP.computeIfAbsent(userId, k -> new AtomicBoolean(false));
        cancelled.set(false);

        ChatClient chatClient = chatClientBuilder.build();

        return chatClient.prompt()
                .system(loadSystemPrompt())
                .user(userInput)
                .stream()
                .content()
                .takeUntil(s -> cancelled.get())
                .doOnCancel(() -> log.info("Agent stream cancelled: userId={}, agent={}", userId, name()))
                .doOnComplete(() -> log.debug("Agent stream completed: userId={}, agent={}", userId, name()))
                .doOnError(e -> log.error("Agent stream error: userId={}, agent={}", userId, name(), e));
    }

    /** 公共非流式处理 */
    @Override
    public String handle(String userInput, Long userId) {
        ChatClient chatClient = chatClientBuilder.build();
        try {
            return chatClient.prompt()
                    .system(loadSystemPrompt())
                    .user(userInput)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Agent handle error: userId={}, agent={}", userId, name(), e);
            return "处理出错：" + e.getMessage();
        }
    }

    /** 取消当前用户的流式生成 */
    public static void cancel(Long userId) {
        AtomicBoolean cb = CANCEL_MAP.get(userId);
        if (cb != null) cb.set(true);
        log.info("Cancel requested for userId={}", userId);
    }
}
