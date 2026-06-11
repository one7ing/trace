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
public class WeeklyReportAgent extends AbstractAgent {

    private String cachedPrompt;

    public WeeklyReportAgent(ChatClient.Builder chatClientBuilder, MemoryService memoryService) {
        super(chatClientBuilder, memoryService);
    }

    @Override
    public String name() { return "weekly_report"; }

    @Override
    public boolean canHandle(String userInput, Long userId) {
        return matchKeywords(userInput, "周报", "总结", "汇总", "本周", "上周");
    }

    @Override
    protected String loadSystemPrompt() {
        if (cachedPrompt == null) {
            try {
                cachedPrompt = new ClassPathResource("agent/prompts/weekly_report.txt")
                        .getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                cachedPrompt = "你是 Trace 系统的 AI 周报生成器。";
            }
        }
        return cachedPrompt;
    }
}
