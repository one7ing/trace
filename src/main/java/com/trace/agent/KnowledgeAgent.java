package com.trace.agent;

import com.trace.enums.IntentType;
import com.trace.service.MemoryService;
import com.trace.service.SearchRouterService;
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
    private final SearchRouterService searchRouter;
    private final RabbitTemplate rabbitTemplate;

    public KnowledgeAgent(ChatClient.Builder chatClientBuilder,
                          MemoryService memoryService,
                          StringRedisTemplate stringRedisTemplate,
                          QueryRewriteAgent queryRewriteAgent,
                          SearchRouterService searchRouter,
                          RabbitTemplate rabbitTemplate) {
        super(chatClientBuilder, memoryService, stringRedisTemplate);
        this.queryRewriteAgent = queryRewriteAgent;
        this.searchRouter = searchRouter;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public String name(){ return "knowledge"; }

    @Override
    public boolean canHandle(String userInput, Long userId) {
        return !matchKeywords(userInput, "刷题", "练习", "题目", "做题", "面试", "出题", "日记", "周报", "计划");
    }

    @Override
    public Flux<String> handleStream(String userInput, Long userId) {
        String rewritten = queryRewriteAgent.rewrite(
                userId, userInput, queryRewriteAgent.getRecentHistory(userId));
        IntentType intent = classifyIntent(userInput);
        log.info("意图分类结果: '{}' → {}", rewritten, intent);

        String systemPrompt;
        if (intent == IntentType.WEB_SEARCH) {
            systemPrompt = loadSystemPrompt()
                    + "\n\n## ⚠️ 用户询问的是需要联网才能回答的实时问题。"
                    + "请调用联网搜索获取最新信息，搜索完成后直接给出答案。"
                    + "绝对不要向用户说【正在搜索】、【调用工具】之类的话。"
                    + "\n\n" + userInput;
        } else {
            StringBuilder kbInfo = new StringBuilder();
            if (intent == IntentType.SEARCH) {
                var searchResult = searchRouter.knowledgeSearch(userId, rewritten, false);
                @SuppressWarnings("unchecked")
                List<Document> docs = (List<Document>) searchResult.get("results");
                if (docs != null && !docs.isEmpty()) {
                    kbInfo.append("\n\n## 检索到的相关知识\n");
                    for (int i = 0; i < Math.min(docs.size(), 5); i++) {
                        kbInfo.append("- ").append(docs.get(i).getFormattedContent()).append("\n");
                    }
                } else {
                    kbInfo.append("\n\n## ⚠️ 知识库无匹配结果。"
                            + "请调用联网搜索获取信息，搜索完成后直接给出答案。"
                            + "不要向用户说正在搜索之类的话。");
                }
            }
            systemPrompt = loadSystemPrompt()
                    + "\n\n" + buildContext(userId, rewritten) + kbInfo;
        }

        memoryService.saveChatHistory(userId, "user", userInput);
        AtomicReference<StringBuilder> acc = new AtomicReference<>(new StringBuilder());

        return chatClientBuilder.build().prompt()
                .system(systemPrompt).user(rewritten)
                .stream().content()
                .takeUntil(token -> stringRedisTemplate.opsForValue()
                        .get(cancelKey(userId)) != null)
                .doOnNext(s -> acc.get().append(s))
                .doOnComplete(() -> {
                    String content = acc.get().toString();
                    if (content.isBlank()) content = "抱歉，未能获取有效回答，请稍后重试。";
                    memoryService.saveChatHistory(userId, "ai", content);
                    triggerMemoryExtractIfNeeded(userId);
                })
                .doOnCancel(() -> {
                    String content = acc.get().toString();
                    if (!content.isBlank()) memoryService.saveChatHistory(userId, "ai", content);
                })
                .doOnError(e -> log.error("KnowledgeAgent流式响应出错: userId={}", userId, e));
    }

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

    private IntentType classifyIntent(String query) {
        if (containsAny(query, "我的笔记", "我上传的", "我的知识库", "根据我的", "我的文档"))
            return IntentType.SEARCH;
        if (containsAny(query, "搜索", "搜一下", "查一下", "上网", "联网", "上网查", "帮我搜", "今天", "实时"))
            return IntentType.WEB_SEARCH;
        return IntentType.DIRECT;
    }

    private boolean containsAny(String query, String... keywords) {
        String lower = query.toLowerCase();
        for (String kw : keywords)
            if (lower.contains(kw.toLowerCase())) return true;
        return false;
    }

    @Override
    protected String loadSystemPrompt() {
        if (cachedPrompt == null) {
            try {
                cachedPrompt = new ClassPathResource("agent/prompts/knowledge.txt")
                        .getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                cachedPrompt = "你是Trace系统的AI知识库，可使用联网搜索获取最新信息。";
            }
        }
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        return "当前日期：" + today + "基于此日期回答用户问题\n\n" + cachedPrompt;
    }
}
