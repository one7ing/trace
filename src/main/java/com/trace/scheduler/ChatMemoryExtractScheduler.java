package com.trace.scheduler;

import com.trace.agent.MemoryExtractAgent;
import com.trace.mapper.UserMapper;
import com.trace.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 聊天记忆延迟提取调度器。
 * <p>
 * 每 10 分钟扫描一次：对有新聊天记录且距上次提取超过 10 分钟的用户，
 * 调用 MemoryExtractAgent 从对话中提取长期记忆特征。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMemoryExtractScheduler {

    private final MemoryService memoryService;
    private final MemoryExtractAgent memoryExtractAgent;
    private final UserMapper userMapper;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_KEY = "distributed:lock:chat-memory-extract";
    private static final String LAST_EXTRACTED_KEY_PREFIX = "chat:last_extracted:";
    /** 提取间隔（分钟） */
    private static final int EXTRACT_INTERVAL_MINUTES = 10;

    /**
     * 每隔 10 分钟执行一次聊天记忆提取。
     */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void extractChatMemories() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(0, 5, TimeUnit.MINUTES)) {
                log.debug("ChatMemoryExtract: another instance is running, skipped");
                return;
            }
            log.info("ChatMemoryExtract: starting scan...");
            var users = userMapper.selectList(null);
            int processed = 0;
            int extracted = 0;

            for (var user : users) {
                Long userId = user.getId();
                try {
                    // 检查是否到达提取时间
                    if (!shouldExtract(userId)) {
                        continue;
                    }
                    processed++;

                    // 获取最近对话上下文
                    List<Map<String, String>> ctx =
                            memoryService.getChatContext(userId);
                    if (ctx == null || ctx.isEmpty()) {
                        updateExtractedTime(userId);
                        continue;
                    }

                    // 拼接对话为文本
                    StringBuilder chatText = new StringBuilder();
                    for (Map<String, String> msg : ctx) {
                        String role = msg.get("role");
                        String content = msg.get("content");
                        if (content != null && !content.isBlank()) {
                            chatText.append("[").append(role).append("] ")
                                    .append(content).append("\n");
                        }
                    }

                    if (chatText.isEmpty()) {
                        updateExtractedTime(userId);
                        continue;
                    }

                    // 提取并写入长期记忆
                    int saved = memoryExtractAgent.extractAndSave(
                            userId, chatText.toString(), "chat_extract");
                    extracted += saved;

                    // 更新提取时间
                    updateExtractedTime(userId);

                } catch (Exception e) {
                    log.error("ChatMemoryExtract: failed for userId={}", userId, e);
                }
            }
            log.info("ChatMemoryExtract: done. processed={}, extracted={}",
                    processed, extracted);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 判断是否应该提取该用户的聊天记忆。
     * 条件：距离上次提取超过 10 分钟，且有新的聊天记录。
     */
    private boolean shouldExtract(Long userId) {
        String key = LAST_EXTRACTED_KEY_PREFIX + userId;
        Object lastObj = redisTemplate.opsForValue().get(key);
        if (lastObj == null) {
            // 从未提取过 → 如果有聊天记录则提取
            List<Map<String, String>> ctx = memoryService.getChatContext(userId);
            return ctx != null && !ctx.isEmpty();
        }
        long lastTime;
        try {
            lastTime = Long.parseLong(lastObj.toString());
        } catch (NumberFormatException e) {
            return true;
        }
        long threshold = System.currentTimeMillis()
                - (EXTRACT_INTERVAL_MINUTES * 60 * 1000L);
        return lastTime < threshold;
    }

    /**
     * 更新最后提取时间为当前时间。
     */
    private void updateExtractedTime(Long userId) {
        String key = LAST_EXTRACTED_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key,
                String.valueOf(System.currentTimeMillis()));
    }
}
