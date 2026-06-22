package com.trace.agent;

import com.trace.entity.LongTermMemory;
import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import com.constant.constant;

@Slf4j
public abstract class AbstractAgent implements Agent {
    protected final ChatClient.Builder chatClientBuilder;
    protected final MemoryService memoryService;
    protected final StringRedisTemplate stringRedisTemplate;

    private static volatile StringRedisTemplate staticRedisTemplate;

    protected AbstractAgent(ChatClient.Builder chatClientBuilder,
                            MemoryService memoryService,
                            StringRedisTemplate stringRedisTemplate) {
        this.chatClientBuilder = chatClientBuilder;
        this.memoryService = memoryService;
        this.stringRedisTemplate = stringRedisTemplate;
        AbstractAgent.staticRedisTemplate = stringRedisTemplate;
    }

    protected abstract String loadSystemPrompt();

    protected boolean matchKeywords(String input, String... keywords) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) return true;
        }
        return false;
    }

    protected String buildContext(Long userId, String userInput) {
        List<LongTermMemory> memories = memoryService.searchSimilarMemories(userId, userInput, 3);
        List<Map<String, String>> shortTermHistory = memoryService.getChatContext(userId);
        StringBuilder sb = new StringBuilder();
        if (memories != null && !memories.isEmpty()) {
            sb.append("## 与当前问题相关的用户记忆：\n");
            for (LongTermMemory m : memories) {
                sb.append("- ").append(m.getContent()).append("\n");
            }
            sb.append("\n");
        }
        if (shortTermHistory != null && !shortTermHistory.isEmpty()) {
            sb.append("## 近期聊天记录：\n");
            for (Map<String, String> entry : shortTermHistory) {
                String role = entry.get("role");
                String content = entry.get("content");
                if (content != null && !content.isBlank()) {
                    if ("user".equals(role)) {
                        sb.append("用户：").append(content).append("\n");
                    } else {
                        sb.append("AI：").append(content).append("\n");
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    static String cancelKey(Long userId) {
        return constant.CANCEL_KEY_PREFIX + userId;
    }

    @Override
    public reactor.core.publisher.Flux<String> handleStream(String userInput, Long userId) {
        ChatClient chatClient = chatClientBuilder.build();
        return chatClient.prompt()
                .system(loadSystemPrompt())
                .user(userInput)
                .stream()
                .content()
                .doFirst(() -> stringRedisTemplate.opsForValue().set(cancelKey(userId), "true"))
                .takeUntil(token -> stringRedisTemplate.opsForValue().get(cancelKey(userId)) != null)
                .doOnCancel(() -> {
                    stringRedisTemplate.delete(cancelKey(userId));
                    log.info("Agent流式响应已取消: userId={}, agent={}", userId, name());
                })
                .doOnComplete(() -> {
                    stringRedisTemplate.delete(cancelKey(userId));
                    log.debug("Agent流式响应已完成: userId={}, agent={}", userId, name());
                })
                .doOnError(e -> {
                    stringRedisTemplate.delete(cancelKey(userId));
                    log.error("Agent流式响应出错: userId={}, agent={}", userId, name(), e);
                });
    }

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
            log.error("Agent.handle调用失败: userId={}, agent={}, 错误位置=AbstractAgent.handle", userId, name(), e);
            return "处理出错：" + e.getMessage();
        }
    }

    public static void cancel(Long userId) {
        StringRedisTemplate redis = staticRedisTemplate;
        redis.delete(cancelKey(userId));
    }
}
