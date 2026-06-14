package com.trace.agent;

import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 长期记忆提取 Agent —— 从周报/聊天记录中提取用户特征摘要。
 * <p>
 * 生成结构化的用户画像要点（偏好、习惯、目标、知识兴趣等），
 * 写入 long_term_memories 表供后续请求注入 Prompt。
 * </p>
 */
@Slf4j
@Component
public class MemoryExtractAgent {

    private final ChatClient.Builder chatClientBuilder;
    private final MemoryService memoryService;

    private static final String EXTRACT_PROMPT = """
            你是一个用户信息提炼助手。根据输入的文本（周报或聊天记录），
            提取该用户的关键特征、偏好、习惯、正在学习的方向、关注的话题等信息。

            规则：
            1. 只提取事实性、长期有效的信息（如"在学习 Spring Boot"），
               不要提取临时性、一次性信息（如"今天面试了某公司"）。
            2. 每条摘要不超过 80 个字，简洁明确。
            3. 输出格式：每条一行，以 "- " 开头，最多输出 5 条。
            4. 不要输出无关的解释或废话。
            5. 如果文本中没有值得提取的长期信息，输出"无"。
            """;

    public MemoryExtractAgent(ChatClient.Builder chatClientBuilder,
                               MemoryService memoryService) {
        this.chatClientBuilder = chatClientBuilder;
        this.memoryService = memoryService;
    }

    /**
     * 从文本中提取长期记忆并写入数据库（自动去重 + 容量控制）。
     *
     * @param userId     用户 ID
     * @param sourceText 来源文本（周报 or 聊天记录）
     * @param sourceType 来源类型（weekly_report / chat_extract）
     * @return 成功写入的条数
     */
    public int extractAndSave(Long userId, String sourceText, String sourceType) {
        if (sourceText == null || sourceText.isBlank()) {
            return 0;
        }
        try {
            String result = chatClientBuilder.build()
                    .prompt()
                    .system(EXTRACT_PROMPT)
                    .user(sourceText)
                    .call()
                    .content();

            if (result == null || result.contains("无")) {
                log.debug("No long-term info extracted for userId={}, sourceType={}",
                        userId, sourceType);
                return 0;
            }

            List<String> items = Arrays.stream(result.split("\n"))
                    .map(String::trim)
                    .filter(line -> line.startsWith("- "))
                    .map(line -> line.substring(2).trim())
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());

            int saved = 0;
            for (String item : items) {
                try {
                    memoryService.saveMemory(userId, item, sourceType);
                    saved++;
                } catch (Exception e) {
                    log.warn("Failed to save memory item: {}", item, e);
                }
            }
            log.info("Memory extracted: userId={}, sourceType={}, saved={}/{}",
                    userId, sourceType, saved, items.size());
            return saved;
        } catch (Exception e) {
            log.error("Memory extraction failed: userId={}", userId, e);
            return 0;
        }
    }
}
