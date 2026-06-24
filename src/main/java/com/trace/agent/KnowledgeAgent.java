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


    /**
     * 根据前端传入的 mode 分流处理。
     */
    public Flux<String> handleStreamWithMode(String userInput, Long userId,
                                              String mode, String knowledgeBaseTopic) {
        // 联网搜索模式：-直接发给 LLM，不带上下文和历史
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
                + "\n\n## ⚠️ 你必须联网搜索获取最新信息来回答用户问题。"
                + "请调用搜索工具获取信息后直接给出答案，"
                + "绝对不要向用户说【正在搜索】、【调用工具】之类的话。";

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
        String systemPrompt = loadSystemPrompt() + "\n\n" + buildContext(userId, userInput) +
                "在回答任何问题之前，请首先仔细阅读‘与当前问题相关的用户记忆’。" +
                "这些记忆是关于用户的最高优先级信息，必须优先采纳。除非记忆明确过期或与当前问题完全无关，否则不要否认或忽略它们。" +
                " 严格禁止（违反即为错误响应）\n" +
                "- 禁止在回答中提及“系统”、“记忆”、“上下文”、“提示词”、“数据库”、“检索”等任何技术词汇。\n" +
                "- 禁止使用“根据我的记忆”、“基于你提供的信息”、“系统记录显示”等任何解释信息来源的句式。\n" +
                "- 禁止说“我不知道”、“我不了解你”、“我无法获取”等否认性语句——如果你看到了用户信息，你就直接用它来回答，不要解释你是怎么知道的。\n" +
                "- 你的回答应该像一个已经认识用户的朋友，自然地使用用户的姓名和已知信息，不要暴露任何技术细节。"+
                "如果消息不在你的知识库，不要编，不要骗用户，直接说无法回答";

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
