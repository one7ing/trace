package com.trace.agent;

import com.trace.service.MemoryService;
import com.trace.service.SearchRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 知识科普 Agent —— 负责联网搜索、知识问答。
 * 集成 MCP 工具（Tavily 联网搜索）、查询改写、流式输出。
 */
@Slf4j
@Component
public class KnowledgeAgent extends AbstractAgent {

    private String cachedPrompt;
    private final QueryRewriteAgent queryRewriteAgent;
    private final SearchRouterService searchRouter;

    public KnowledgeAgent(ChatClient.Builder chatClientBuilder,
                          MemoryService memoryService,
                          QueryRewriteAgent queryRewriteAgent,
                          SearchRouterService searchRouter) {
        super(chatClientBuilder, memoryService);
        this.queryRewriteAgent = queryRewriteAgent;
        this.searchRouter = searchRouter;
    }

    @Override public String name() { return "knowledge"; }

    @Override
    public boolean canHandle(String userInput, Long userId) {
        return !matchKeywords(userInput, "面试", "出题", "日记", "周报", "计划");
    }

    @Override
    public Flux<String> handleStream(String userInput, Long userId) {
        String rewritten = queryRewriteAgent.rewrite(
                userId, userInput, queryRewriteAgent.getRecentHistory(userId));

        // 意图判断：WEB_SEARCH（联网）→ SEARCH（RAG）→ DIRECT（直接回答）
        String intent = classifyIntent(rewritten);
        log.info("Intent classified: '{}' → {}", rewritten, intent);

        if ("DIRECT".equals(intent)) {
            // 直接回答，不走 RAG
            String context = buildContext(userId, rewritten);
            memoryService.saveChatHistory(userId, "user", userInput);
            AtomicReference<StringBuilder> acc = new AtomicReference<>(new StringBuilder());
            return chatClientBuilder.build().prompt()
                    .system(loadSystemPrompt() + "\n\n" + context)
                    .user(rewritten)
                    .stream().content()
                    .doOnNext(s -> acc.get().append(s))
                    .doOnComplete(() -> {
                        memoryService.saveChatHistory(userId, "ai",
                                acc.get().toString());
                    })
                    .doOnError(e -> log.error("Direct answer error", e));
        }


        if ("WEB_SEARCH".equals(intent)) {
            log.info("Web search (function calling): userId={}, query={}", userId, rewritten);
            String context = buildContext(userId, rewritten);
            memoryService.saveChatHistory(userId, "user", userInput);
            AtomicReference<StringBuilder> acc = new AtomicReference<>(new StringBuilder());
            var prompt = chatClientBuilder.build().prompt()
                    .system(loadSystemPrompt()
                            + "\n\n## 用户需要实时/最新信息，"
                            + "请调用 tavily_search 工具联网搜索后回答。"
                            + "\n\n" + context)
                    .user(rewritten);
            return prompt.stream().content()
                    .doOnNext(s -> acc.get().append(s))
                    .doOnComplete(() -> memoryService.saveChatHistory(
                            userId, "ai", acc.get().toString()))
                    .doOnError(e -> log.error("Web search error", e));
        }
        // SEARCH → RAG 流程（先查本地知识库，无结果再联网）
        var searchResult = searchRouter.knowledgeSearch(userId, rewritten, false);
        @SuppressWarnings("unchecked")
        List<org.springframework.ai.document.Document> docs =
                (List<org.springframework.ai.document.Document>)
                        searchResult.get("results");
        boolean fromWeb = (boolean) searchResult.getOrDefault("fromWeb", false);

        StringBuilder kbInfo = new StringBuilder();
        if (docs != null && !docs.isEmpty()) {
            kbInfo.append("\n\n## 检索到的相关知识\n");
            for (int i = 0; i < Math.min(docs.size(), 5); i++) {
                kbInfo.append("- ").append(
                        docs.get(i).getFormattedContent()).append("\n");
            }
        } else if (!fromWeb) {
            kbInfo.append("\n\n## 知识库无匹配结果，请联网搜索\n");
        }

        String context = buildContext(userId, rewritten) + kbInfo;
        var prompt = chatClientBuilder.build().prompt()
                .system(loadSystemPrompt() + "\n\n" + context)
                .user(rewritten);

        memoryService.saveChatHistory(userId, "user", userInput);
        AtomicReference<StringBuilder> acc = new AtomicReference<>(new StringBuilder());
        return prompt.stream().content()
                .takeUntil(s -> {
                    var cb = CANCEL_MAP.get(userId);
                    return cb != null && cb.get();
                })
                .doOnNext(s -> acc.get().append(s))
                .doOnComplete(() -> {
                    memoryService.saveChatHistory(userId, "ai",
                            acc.get().toString());
                });
    }

    /**
     * 意图分类：WEB_SEARCH（强制联网）→ SEARCH（需检索）→ DIRECT（直接回答）。
     * <p>
     * 先通过关键词快速识别明确需要联网的场景，避免不必要的 AI 调用。
     * 关键词未命中时再调用 AI 进行 SEARCH / DIRECT 二分。
     * </p>
     */
    private String classifyIntent(String query) {
        if (query == null) {
            return "DIRECT";
        }

        // 1. 明确指定要搜索网络
        if (containsAny(query, "搜索", "搜一下", "查一下", "上网", "联网", "上网查", "帮我搜")) {
            return "WEB_SEARCH";
        }
        // 3. 实时信息 —— 金融/天气/新闻等必须联网的场景
        if (containsAny(query, "股票", "股价", "大盘", "涨停", "跌停",
                "汇率", "比特币", "以太坊", "加密货币", "黄金价格", "原油",
                "天气", "温度", "台风", "地震",
                "最新新闻", "刚刚", "今天发生")) {
            return "WEB_SEARCH";
        }

        // 4. 其他情况交给 AI 判断 SEARCH vs DIRECT
        try {
            String result = chatClientBuilder.build().prompt()
                    .user(query)
                    .system("""
                            判断用户问题是否需要查询外部知识库才能准确回答。
                            - 实时信息、特定文档、最新新闻、用户个人数据、
                              最近事件 → 回复 SEARCH
                            - 通用知识、编程、数学、翻译、闲聊、逻辑推理 →
                              回复 DIRECT
                            只回复一个单词。
                            """)
                    .call().content();
            return result != null && result.trim().toUpperCase().contains("DIRECT")
                    ? "DIRECT" : "SEARCH";
        } catch (Exception e) {
            return "SEARCH";
        }
    }

    /** 判断 query 是否包含任意关键词（忽略大小写）。 */
    private boolean containsAny(String query, String... keywords) {
        String lower = query.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String loadSystemPrompt() {
        if (cachedPrompt == null) {
            try {
                cachedPrompt = new ClassPathResource("agent/prompts/knowledge.txt")
                        .getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                cachedPrompt = "你是Trace系统的AI知识库。可使用Tavily联网搜索获取最新信息。";
            }
        }
        return cachedPrompt;
    }
}
