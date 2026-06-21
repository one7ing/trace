package com.trace.agent;

import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class InterviewAgent extends AbstractAgent {

    private String cachedPrompt;

    public InterviewAgent(ChatClient.Builder chatClientBuilder,
                          MemoryService memoryService,
                          StringRedisTemplate stringRedisTemplate) {
        super(chatClientBuilder, memoryService, stringRedisTemplate);
    }

    @Override
    public String name() { return "interview"; }

    @Override
    public boolean canHandle(String userInput, Long userId) {
        return matchKeywords(userInput, "面试", "出题", "评分", "评估");
    }

    @Override
    public String handle(String userInput, Long userId) {
        String context = buildContext(userId, userInput);
        ChatClient chatClient = chatClientBuilder.build();
        return chatClient.prompt()
                .system(loadSystemPrompt() + "\n\n" + context)
                .user(userInput)
                .call()
                .content();
    }

    @Override
    protected String loadSystemPrompt() {
        if (cachedPrompt == null) {
            try {
                cachedPrompt = new ClassPathResource("agent/prompts/interview.txt")
                        .getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                cachedPrompt = "你是 Trace 系统的 AI 面试教练。";
            }
        }
        return cachedPrompt;
    }
}
