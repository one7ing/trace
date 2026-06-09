package com.trace.service;

import com.trace.config.McpConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final ChatClient.Builder chatClientBuilder;
    private final MemoryService memoryService;
    private final McpConfig mcpConfig;

    private static final String DISCLAIMER =
            "\n\n---\n⚠️ 以上内容基于互联网搜索结果整理，仅供参考。AI 可能会出错，请保持独立判断。";

    private static final String SYSTEM_PROMPT = """
            你是 Trace 系统的 AI 知识库。你的职责是：
            1. 基于提供的互联网搜索结果，客观、准确地回答用户的专业知识问题
            2. 如果搜索结果不足以回答问题，请明确告知用户
            3. 回答末尾必须附加免责声明
            4. 引用来源时注明参考链接
            5. 语言简洁专业，避免过度延伸
            """;

    /**
     * 流式知识问答
     */
    public Flux<String> chatStream(Long userId, String message, String domain) {
        // 1. 检索长期记忆
        List<com.trace.entity.LongTermMemory> memories = memoryService.retrieveMemories(userId, message, 5);

        // 2. 构建 Prompt
        StringBuilder contextBuilder = new StringBuilder(SYSTEM_PROMPT);
        if (!memories.isEmpty()) {
            contextBuilder.append("\n\n## 用户历史相关记忆：\n");
            for (var mem : memories) {
                contextBuilder.append("- ").append(mem.getContent()).append("\n");
            }
        }
        if (domain != null && !domain.isEmpty()) {
            contextBuilder.append("\n\n## 用户关注的知识领域：").append(domain);
        }

        // 3. 调用大模型流式输出
        ChatClient chatClient = chatClientBuilder.build();

        return chatClient.prompt()
                .system(contextBuilder.toString())
                .user(message)
                .advisors(new SimpleLoggerAdvisor())
                .stream()
                .content()
                .concatWithValues(DISCLAIMER)
                .doOnComplete(() -> {
                    // 4. 保存上下文到短期记忆
                    memoryService.saveChatContext(userId, "user", message);
                })
                .doOnError(e -> log.error("Knowledge chat error", e));
    }

    /**
     * 保存知识问答摘要到长期记忆
     */
    public void saveKnowledgeMemory(Long userId, String question, String summary) {
        String content = "【知识问答】Q: " + question + " | A: " + summary;
        memoryService.saveLongTermMemory(userId, content, "knowledge", null);
    }
}
