package com.trace.Consumer;

import com.trace.agent.MemoryExtractAgent;
import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.constant.constant.ChatMemoryExtract.*;
import static com.constant.constant.RabbitMQ.MEMORY_EXTRACT_QUEUE;

@Slf4j
@Component
public class MemoryExtractConsumer {

    private final MemoryService memoryService;
    private final MemoryExtractAgent memoryExtractAgent;
    private final StringRedisTemplate stringRedisTemplate;

    public MemoryExtractConsumer(MemoryService memoryService,
                                  MemoryExtractAgent memoryExtractAgent,
                                  StringRedisTemplate stringRedisTemplate) {
        this.memoryService = memoryService;
        this.memoryExtractAgent = memoryExtractAgent;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @RabbitListener(queues = MEMORY_EXTRACT_QUEUE)
    public void handleMemoryExtract(Map<String, Object> msg) {
        Long userId = Long.valueOf(msg.get("userId").toString());
        String lockKey = LOCK_KEY_PREFIX + userId;

        // SETnx 幂等性判断：如果 key 已存在，说明该用户的消息正在被处理或是重复消息
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        if (acquired == null || !acquired) {
            log.info("记忆提取消息重复，已跳过: userId={}", userId);
            return;
        }

        log.info("RabbitMQ消费者: 开始提取记忆 userId={}", userId);
        try {
            // 增量提取：只取最近 RECENT_MESSAGE_COUNT 条对话
            List<Map<String, String>> ctx = memoryService.getChatContext(userId, RECENT_MESSAGE_COUNT);
            if (ctx == null || ctx.isEmpty()) {
                log.debug("短期上下文为空，跳过记忆提取: userId={}", userId);
                return;
            }
            StringBuilder chatText = new StringBuilder();
            for (Map<String, String> entry : ctx) {
                String role = entry.get("role"), content = entry.get("content");
                if (content != null && !content.isBlank())
                    chatText.append("[").append(role).append("] ").append(content).append("\n");
            }
            if (chatText.isEmpty()) {
                log.debug("聊天文本为空，跳过记忆提取: userId={}", userId);
                return;
            }
            int saved = memoryExtractAgent.extractAndSave(userId, chatText.toString(), "chat_extract");
            log.info("RabbitMQ消费者: 记忆提取完成 userId={}, 保存条数={}", userId, saved);

            // 消费者成功处理后才更新时间戳
            String timestampKey = LAST_EXTRACTED_KEY_PREFIX + userId;
            stringRedisTemplate.opsForValue()
                    .set(timestampKey, String.valueOf(System.currentTimeMillis()));
            log.debug("已更新记忆提取时间戳: userId={}", userId);
        } catch (Exception e) {
            log.error("RabbitMQ消费者: 记忆提取失败 userId={}, 错误位置=MemoryExtractConsumer.handleMemoryExtract", userId, e);
        } finally {
            // 释放锁
            stringRedisTemplate.delete(lockKey);
        }
    }
}
