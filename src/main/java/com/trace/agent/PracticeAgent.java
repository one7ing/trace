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
public class PracticeAgent extends AbstractAgent {

    private String cachedPrompt;

    public PracticeAgent(ChatClient.Builder chatClientBuilder,
                         MemoryService memoryService,
                         StringRedisTemplate stringRedisTemplate) {
        super(chatClientBuilder, memoryService, stringRedisTemplate);
    }

    @Override
    public String name() { return "practice"; }

    @Override
    public boolean canHandle(String userInput, Long userId) {
        return matchKeywords(userInput, "刷题", "练习", "题目", "做题");
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
                cachedPrompt = new ClassPathResource("agent/prompts/practice.txt")
                        .getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                cachedPrompt = "你是 Trace 系统的刷题助手，帮助用户练习技术题目。";
            }
        }
        return cachedPrompt;
    }
}
