package com.trace.service.impl;

import com.trace.agent.MemoryExtractAgent;
import com.trace.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

import static com.constant.constant.RabbitMQ.MEMORY_EXTRACT_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryExtractConsumer {

    private final MemoryService memoryService;
    private final MemoryExtractAgent memoryExtractAgent;

    @RabbitListener(queues = MEMORY_EXTRACT_QUEUE)
    public void handleMemoryExtract(Map<String, Object> msg) {
        Long userId = Long.valueOf(msg.get("userId").toString());
        log.info("RabbitMQ: extracting memory for userId={}", userId);
        try {
            List<Map<String, String>> ctx = memoryService.getChatContext(userId);
            if (ctx == null || ctx.isEmpty()) return;
            StringBuilder chatText = new StringBuilder();
            for (Map<String, String> entry : ctx) {
                String role = entry.get("role"), content = entry.get("content");
                if (content != null && !content.isBlank()) chatText.append("[").append(role).append("] ").append(content).append("\n");
            }
            if (chatText.isEmpty()) return;
            int saved = memoryExtractAgent.extractAndSave(userId, chatText.toString(), "chat_extract");
            log.info("RabbitMQ: memory extraction done userId={}, saved={}", userId, saved);
        } catch (Exception e) {
            log.error("RabbitMQ: memory extraction failed userId={}", userId, e);
        }
    }
}
