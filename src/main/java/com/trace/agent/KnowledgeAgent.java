package com.trace.agent;

import com.trace.service.MemoryService;
import com.trace.service.KnowledgeBaseService;
import com.trace.entity.KnowledgeBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.constant.constant.ChatMemoryExtract.*;
import static com.constant.constant.RabbitMQ.*;

@Slf4j
@Component
public class KnowledgeAgent extends AbstractAgent {

    private String cachedPrompt;
    private final QueryRewriteAgent queryRewriteAgent;
    private final KnowledgeBaseService kbService;
    private final RabbitTemplate rabbitTemplate;

    public KnowledgeAgent(ChatClient.Builder chatClientBuilder,
                          MemoryService memoryService,
                          StringRedisTemplate stringRedisTemplate,
                          QueryRewriteAgent queryRewriteAgent,
                          KnowledgeBaseService kbService,
                          RabbitTemplate rabbitTemplate) {
        super(chatClientBuilder, memoryService, stringRedisTemplate);
        this.queryRewriteAgent = queryRewriteAgent;
        this.kbService = kbService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public String name() { return "knowledge"; }

    @Override
    public boolean canHandle(String userInput, Long userId) {
        return !matchKeywords(userInput, "刷题", "练习", "题目", "做题", "面试", "出题", "日记", "周报", "计划");
    }

    @Override
    public Flux<String> handleStream(String userInput, Long userId) {
        // 默认 direct 模式
        return handleStreamWithMode(userInput, userId, "direct", null);
    }

    /**
     * 根据前端传入的 mode 分流处理。
     */
    public Flux<String> handleStreamWithMode(String userInput, Long userId,
                                              String mode, String knowledgeBaseTopic) {
        // 联网搜索模式：重写 prompt 直接发给 LLM，不带上下文和历史
        if ("web".equals(mode)) {
            return handleWebSearch(userInput, userId);
        }
        // RAG 知识库模式：检索指定知识库后回答
        if ("rag".equals(mode)) {
            return handleRagSearch(userInput, userId, knowledgeBaseTopic);
        }
        // 默认直接对话模式
        return handleDirect(userInput, userId);
    }

    /** 联网搜索：重写 prompt → 直接发给 LLM */
    private Flux<String> handleWebSearch(String userInput, Long userId) {
        String rewritten = queryRewriteAgent.rewrite(userId, userInput, List.of());
        String systemPrompt = loadSystemPrompt()
                + "\n\n## ⚠️ 你需要联网搜索获取最新信息来回答用户问题。"
                + "请调用搜索工具获取信息后直接给出答案，"
                + "绝对不要向用户说【正在搜索】、【调用工具】之类的话。"
                + "做一个免责声明";

        memoryService.saveChatHistory(userId, "user", userInput);
        AtomicReference<StringBuilder> acc = new AtomicReference<>(new StringBuilder());

        return chatClientBuilder.build().prompt()
                .system(systemPrompt).user(rewritten)
                .stream().content()
                .takeUntil(token -> stringRedisTemplate.opsForValue()
                        .get(cancelKey(userId)) != null)
                .doOnNext(s -> acc.get().append(s))
                .doOnComplete(() -> finishChat(userId, acc))
                .doOnCancel(() -> finishChat(userId, acc))
                .doOnError(e -> log.error("KnowledgeAgent联网模式出错: userId={}", userId, e));
    }

    /** RAG 知识库：按分类选择检索方式 → 有则回答，无则直接告知 */
    private Flux<String> handleRagSearch(String userInput, Long userId, String kbTopic) {
        String rewritten = queryRewriteAgent.rewrite(userId, userInput, List.of());

        // 查出该知识库的 category
        List<KnowledgeBase> items = kbService.listItems(userId, "", 0, 100).getRecords();
        String category = "专业知识问答";
        for (KnowledgeBase kb : items) {
            if (kb.getFileName().equals(kbTopic)) {
                category = kb.getCategory() != null ? kb.getCategory() : category;
                break;
            }
        }

        // 按分类选择检索方式
        List<org.springframework.ai.document.Document> docs;
        if ("闲聊问答".equals(category)) {
            // 闲聊：语义检索 topK=4 threshold=0.6
            docs = kbService.semanticSearch(userId, rewritten, category);
        } else {
            // 专业：混合检索
            docs = kbService.hybridSearch(userId, rewritten, category, 4);
        }

        // 无结果：直接告知用户
        if (docs == null || docs.isEmpty()) {
            memoryService.saveChatHistory(userId, "user", userInput);
            String noResult = "知识库「" + kbTopic + "」中暂无与「" + userInput + "」相关的内容。"
                    + "请尝试换个问法，或上传更多文档到知识库。";
            memoryService.saveChatHistory(userId, "ai", noResult);
            return Flux.just("data:" + noResult + "\n\n");
        }

        // 有结果：构建上下文
        StringBuilder kbInfo = new StringBuilder();
        kbInfo.append("\n\n## 从知识库「").append(kbTopic).append("」检索到的相关内容\n");
        for (int i = 0; i < Math.min(docs.size(), 5); i++) {
            String content = docs.get(i).getText();
            if (content != null && !content.isBlank()) {
                kbInfo.append("- ").append(content, 0, Math.min(content.length(), 500)).append("\n");
            }
        }
        String systemPrompt = loadSystemPrompt()
                + "\n\n## ⚠️ 请根据以下知识库内容回答用户问题。"
                + "如果知识库内容不足以回答问题，请如实告知，不要编造。"
                + kbInfo;

        memoryService.saveChatHistory(userId, "user", userInput);
        AtomicReference<StringBuilder> acc = new AtomicReference<>(new StringBuilder());

        return chatClientBuilder.build()
                .prompt()
                .system(systemPrompt).user(rewritten)
                .stream().content()
                .takeUntil(token -> stringRedisTemplate.opsForValue()
                        .get(cancelKey(userId)) != null)
                .doOnNext(s -> acc.get().append(s))
                .doOnComplete(() -> finishChat(userId, acc))
                .doOnCancel(() -> finishChat(userId, acc))
                .doOnError(e -> log.error("KnowledgeAgent RAG模式出错: userId={}", userId, e));
    }

    /** 默认直接对话：使用 LLM 自身知识 + 上下文 */
    private Flux<String> handleDirect(String userInput, Long userId) {
        String systemPrompt = loadSystemPrompt() + "\n\n" + buildContext(userId, userInput);

        memoryService.saveChatHistory(userId, "user", userInput);
        AtomicReference<StringBuilder> acc = new AtomicReference<>(new StringBuilder());

        return chatClientBuilder.build().prompt()
                .system(systemPrompt).user(userInput)
                .stream().content()
                .takeUntil(token -> stringRedisTemplate.opsForValue()
                        .get(cancelKey(userId)) != null)
                .doOnNext(s -> acc.get().append(s))
                .doOnComplete(() -> finishChat(userId, acc))
                .doOnCancel(() -> finishChat(userId, acc))
                .doOnError(e -> log.error("KnowledgeAgent直接模式出错: userId={}", userId, e));
    }

    /** 保存对话 + 触发记忆提取 */
    private void finishChat(Long userId, AtomicReference<StringBuilder> acc) {
        String content = acc.get().toString();
        if (content.isBlank()) content = "抱歉，未能获取有效回答，请稍后重试。";
        memoryService.saveChatHistory(userId, "ai", content);
        triggerMemoryExtractIfNeeded(userId);
    }

    /** 异步触发记忆提取 */
    private void triggerMemoryExtractIfNeeded(Long userId) {
        try {
            String key = LAST_EXTRACTED_KEY_PREFIX + userId;
            String lastObj = stringRedisTemplate.opsForValue().get(key);
            long now = System.currentTimeMillis();
            long threshold = now - (EXTRACT_INTERVAL_MINUTES * 60 * 1000L);
            if (lastObj != null) {
                try {
                    if (Long.parseLong(lastObj) >= threshold) return;
                } catch (NumberFormatException ignored) {}
            }
            rabbitTemplate.convertAndSend(MEMORY_EXTRACT_EXCHANGE,
                    MEMORY_EXTRACT_ROUTING_KEY, Map.of("userId", userId));
            log.info("记忆提取消息已发送: userId={}", userId);
        } catch (Exception e) {
            log.warn("发送记忆提取消息失败: userId={}", userId, e);
        }
    }

    @Override
    protected String loadSystemPrompt() {
        if (cachedPrompt == null) {
            try {
                cachedPrompt = new ClassPathResource("agent/prompts/knowledge.txt")
                        .getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                cachedPrompt = "你是Trace系统的AI助手，请专业、准确地回答用户问题。";
            }
        }
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        return "当前日期：" + today + "。基于此日期回答用户问题。\n\n" + cachedPrompt;
    }
}
