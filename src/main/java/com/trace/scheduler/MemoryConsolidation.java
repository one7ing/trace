package com.trace.scheduler;


import com.trace.entity.LongTermMemory;
import com.trace.mapper.LongTermMemoryMapper;
import com.trace.mapper.UserMapper;
import com.trace.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor

public class MemoryConsolidation {
    private final LongTermMemoryMapper memoryMapper;
    private final MemoryService memoryService;
    private final ChatClient.Builder chatClientBuilder;
    private static final int MIN_MEMORIES_TO_CONSOLIDATE = 3;
    @Transactional
    public boolean consolidateUser(Long userId) {
        // 获取该用户所有长期记忆
        List<LongTermMemory> all = memoryMapper.findRecentByUserId(userId, 200);
        if (all.size() < MIN_MEMORIES_TO_CONSOLIDATE) {
            log.debug("用户{}记忆数量不足({}条)，跳过整合", userId, all.size());
            return false;
        }

        // 拼接所有记忆内容
        StringBuilder memories = new StringBuilder();
        for (int i = 0; i < all.size(); i++) {
            LongTermMemory m = all.get(i);
            if (m.getContent() != null && !m.getContent().isBlank()) {
                memories.append((i + 1)).append(". ").append(m.getContent()).append("\n");
            }
        }

        // 调用 LLM 整合
        String prompt = "以下是用户的多条长期记忆，请将其整合为一份简洁的用户画像摘要，"
                + "包含：关键信息、偏好、习惯、知识领域、重要经历等。"
                + "输出格式为纯文本，不超过500字。\n\n"
                + memories.toString();

        String summary = chatClientBuilder.build().prompt()
                .system("你是一个个人知识管理助手，专门整理和浓缩用户的长期记忆。")
                .user(prompt)
                .call().content();

        if (summary == null || summary.isBlank()) {
            log.warn("用户{}记忆整合LLM返回为空", userId);
            return false;
        }

        // 删除旧记忆 + 写入整合结果
        int deleted = memoryMapper.deleteAllByUserId(userId);
        memoryService.saveMemory(userId, "【记忆整合 " + java.time.LocalDate.now() + "】\n" + summary,
                "consolidation");
        log.info("用户{}记忆整合完成: 删除{}条旧记忆 → 写入1条整合摘要", userId, deleted);
        return true;
    }
}
