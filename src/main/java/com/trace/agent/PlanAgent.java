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
public class PlanAgent extends AbstractAgent {

    private String cachedPrompt;

    public PlanAgent(ChatClient.Builder chatClientBuilder,
                     MemoryService memoryService,
                     StringRedisTemplate stringRedisTemplate) {
        super(chatClientBuilder, memoryService, stringRedisTemplate);
    }

    @Override
    public String name() { return "plan"; }


    @Override
    protected String loadSystemPrompt() {
        if (cachedPrompt == null) {
            try {
                cachedPrompt = new ClassPathResource("agent/prompts/plan.txt")
                        .getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                cachedPrompt = "你是 Trace 系统的 AI 学习计划教练。";
            }
        }
        return cachedPrompt;
    }
}
