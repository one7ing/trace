package com.trace.scheduler;

import com.trace.entity.LongTermMemory;
import com.trace.mapper.LongTermMemoryMapper;
import com.trace.mapper.UserMapper;
import com.trace.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 长期记忆定时整合 —— 每周日凌晨 0:00 执行，
 * 将每个用户的所有长期记忆浓缩为结构化摘要。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryConsolidationScheduler {

    private final MemoryConsolidation memoryConsolidation;
    private final UserMapper userMapper;

    @Scheduled(cron = "0 0 0 * * SUN")
    public void consolidateAllUsers() {
        log.info("=== 长期记忆整合定时任务开始 ===");
        // 获取所有用户ID
        List<Long> userIds = userMapper.selectAllIds();
        int consolidated = 0;
        for (Long userId : userIds) {
            try {
                if (memoryConsolidation.consolidateUser(userId))
                    consolidated++;
            } catch (Exception e) {
                log.error("用户{}记忆整合失败", userId, e);
            }
        }
        log.info("=== 长期记忆整合完成，共处理{}个用户 ===", consolidated);
    }


}
