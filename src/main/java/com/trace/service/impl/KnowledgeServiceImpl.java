package com.trace.service.impl;

import com.trace.service.KnowledgeService;
import com.trace.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final ChatClient.Builder chatClientBuilder;
    private final MemoryService memoryService;

    private static final String SYSTEM_PROMPT = """
            你是 Trace 系统的 AI 知识库。你的职责是：
            1. 可使用 Brave Search 工具进行联网搜索，获取最新信息
            2. 基于搜索结果客观、准确地回答用户问题
            3. 如果搜索结果不足以回答问题，请明确告知用户
            4. 引用来源时注明参考链接
            5. 语言简洁专业，避免过度延伸
            """;

    @Override
    public Flux<String> chatStream(Long userId, String message, String domain) {
        var memories = memoryService.getRecentMemories(userId, 5);

        StringBuilder contextBuilder = new StringBuilder(SYSTEM_PROMPT);

        if (!memories.isEmpty()) {
            contextBuilder.append("\n\n## 用户历史相关记忆：\n");
            for (var m : memories) {
                contextBuilder.append("- ").append(m.getContent()).append("\n");
            }
        }
        if (domain != null && !domain.isEmpty()) {
            contextBuilder.append("\n\n## 用户关注的知识领域：").append(domain);
        }

        ChatClient chatClient = chatClientBuilder.build();
        return chatClient.prompt()
                .system(contextBuilder.toString())
                .user(message)
                .advisors(new SimpleLoggerAdvisor())
                .stream()
                .content()
                .doOnComplete(() -> memoryService.saveChatHistory(userId, "user", message))
                .doOnError(e -> log.error("Knowledge chat error", e));
    }
}
