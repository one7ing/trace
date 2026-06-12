package com.trace.agent;

import com.trace.enums.SearchType;
import com.trace.service.MemoryService;
import com.trace.service.SearchRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 知识科普 Agent —— 负责联网搜索、知识问答。
 * 集成 MCP 工具（Tavily 联网搜索）、查询改写、流式输出。
 */
@Slf4j
@Component
public class KnowledgeAgent extends AbstractAgent {

    private String cachedPrompt;
    private final ToolCallbackProvider toolCallbackProvider;
    private final QueryRewriteAgent queryRewriteAgent;
    private final SearchRouterService searchRouter;

    public KnowledgeAgent(ChatClient.Builder chatClientBuilder,
                          MemoryService memoryService,
                          @Autowired(required = false) ToolCallbackProvider toolCallbackProvider,
                          QueryRewriteAgent queryRewriteAgent,
                          SearchRouterService searchRouter) {
        super(chatClientBuilder, memoryService);
        this.toolCallbackProvider = toolCallbackProvider;
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
        boolean forceWeb = rewritten.contains("搜索") || rewritten.contains("查一下")
                || rewritten.contains("上网") || rewritten.contains("联网")
                || rewritten.contains("搜一下");

        var searchResult = searchRouter.knowledgeSearch(userId, rewritten, forceWeb);
        @SuppressWarnings("unchecked")
        List<org.springframework.ai.document.Document> docs =
                (List<org.springframework.ai.document.Document>) searchResult.get("results");
        boolean fromWeb = (boolean) searchResult.getOrDefault("fromWeb", false);

        StringBuilder kbInfo = new StringBuilder();
        if (docs != null && !docs.isEmpty()) {
            kbInfo.append("\n\n## 检索到的相关知识\n");
            for (int i = 0; i < Math.min(docs.size(), 5); i++) {
                kbInfo.append("- ").append(docs.get(i).getFormattedContent()).append("\n");
            }
        } else if (!fromWeb) {
            kbInfo.append("\n\n## 知识库无匹配结果，请联网搜索\n");
        }

        String context = buildContext(userId, rewritten) + kbInfo;
        ChatClient chatClient = chatClientBuilder.build();
        var prompt = chatClient.prompt()
                .system(loadSystemPrompt() + "\n\n" + context)
                .user(rewritten);

        if ((docs == null || docs.isEmpty()) && toolCallbackProvider != null) {
            var callbacks = toolCallbackProvider.getToolCallbacks();
            if (callbacks != null && callbacks.length > 0) {
                prompt.toolCallbacks(callbacks);
            }
        }

        return prompt.stream().content()
                .takeUntil(s -> {
                    var cb = CANCEL_MAP.get(userId);
                    return cb != null && cb.get();
                })
                .doOnCancel(() -> log.info("KnowledgeAgent cancelled: userId={}", userId))
                .doOnComplete(() -> memoryService.saveChatContext(userId, "user", userInput))
                .doOnError(e -> log.error("KnowledgeAgent error: userId={}", userId, e));
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
