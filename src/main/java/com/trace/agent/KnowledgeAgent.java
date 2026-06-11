package com.trace.agent;

import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class KnowledgeAgent extends AbstractAgent {

    private String cachedPrompt;

    public KnowledgeAgent(ChatClient.Builder chatClientBuilder,
                          MemoryService memoryService) {
        super(chatClientBuilder, memoryService);
    }

    @Override
    public String name() { return "knowledge"; }

    @Override
    public boolean canHandle(String userInput, Long userId) {
        return !matchKeywords(userInput, "面试", "出题", "日记", "周报", "计划");
    }

    @Override
    public Flux<String> handleStream(String userInput, Long userId) {
        String context = buildContext(userId, userInput);
        ChatClient chatClient = chatClientBuilder.build();

        return chatClient.prompt()
                .system(loadSystemPrompt() + "\n\n" + context)
                .user(userInput)
                .stream()
                .content()
                .takeUntil(s -> {
                    var cb = CANCEL_MAP.get(userId);
                    return cb != null && cb.get();
                })
                .doOnCancel(() -> log.info("KnowledgeAgent stream cancelled: userId={}", userId))
                .doOnComplete(() -> memoryService.saveChatContext(userId, "user", userInput))
                .doOnError(e -> log.error("KnowledgeAgent error: userId={}", userId, e));
    }

    @Override
    protected String loadSystemPrompt() {
        if (cachedPrompt == null) {
            try {
                cachedPrompt = new ClassPathResource("agent/prompts/knowledge.txt")
                        .getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                cachedPrompt = "你是 Trace 系统的 AI 知识库，可以使用 Brave Search 联网搜索获取最新信息。";
            }
        }
        return cachedPrompt;
    }
}
