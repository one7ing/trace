package com.trace.agent;

import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DiaryAgent extends AbstractAgent {

    private String cachedPrompt;

    public DiaryAgent(ChatClient.Builder chatClientBuilder,
                      MemoryService memoryService) {
        super(chatClientBuilder, memoryService);
    }

    @Override
    public String name() { return "diary"; }

    @Override
    public boolean canHandle(String userInput, Long userId) {
        return matchKeywords(userInput, "日记", "心情", "回顾", "记录");
    }

    @Override
    protected String loadSystemPrompt() {
        if (cachedPrompt == null) {
            try {
                cachedPrompt = new ClassPathResource("agent/prompts/diary.txt")
                        .getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                cachedPrompt = "你是 Trace 系统的 AI 日记助手。";
            }
        }
        return cachedPrompt;
    }
}
