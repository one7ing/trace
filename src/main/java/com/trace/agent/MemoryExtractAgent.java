package com.trace.agent;

import com.trace.service.MemoryService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class MemoryExtractAgent {

    private final ChatClient.Builder chatClientBuilder;
    private final MemoryService memoryService;
    private final EmbeddingModel embeddingModel;
    private String cachedPrompt;

    public MemoryExtractAgent(ChatClient.Builder chatClientBuilder,
                               MemoryService memoryService,
                               EmbeddingModel embeddingModel) {
        this.chatClientBuilder = chatClientBuilder;
        this.memoryService = memoryService;
        this.embeddingModel = embeddingModel;
    }

    public int extractAndSave(Long userId, String sourceText, String sourceType) {
        if (sourceText == null || sourceText.isBlank()) return 0;
        try {
            String result = chatClientBuilder
                    .build()
                    .prompt()
                    .system(cachedPrompt)
                    .user(sourceText)
                    .call()
                    .content();
            if (result == null || result.contains("无")) { log.debug("无可记录内容"); return 0; }
            List<String> items = Arrays.stream(result.split("\n")).map(String::trim)
                    .filter(line -> line.startsWith("- ")).map(line -> line.substring(2).trim())
                    .filter(line -> !line.isEmpty()).toList();
            int saved = 0;
            for (String item : items) {
                try {
                    String embedding = null;
                    if (embeddingModel != null) {
                        try { float[] emb = embeddingModel.embed(item); embedding = vectorToString(emb); }
                        catch (Exception e) { log.warn("生成embedding向量失败: MemoryExtractAgent.extractAndSave", e); }
                    }
                    memoryService.saveMemory(userId, item, sourceType, embedding);
                    saved++;
                } catch (Exception e) { log.warn("保存单条记忆失败: MemoryExtractAgent.extractAndSave", e); }
            }
            log.info("记忆提取完成: userId={}, 来源类型={}, 保存成功{}/{}", userId, sourceType, saved, items.size());
            return saved;
        } catch (Exception e) { log.error("记忆提取整体失败: userId={}, 错误位置=MemoryExtractAgent.extractAndSave", userId, e); return 0; }
    }

    @PostConstruct
    private String loadsystem() {
        if (cachedPrompt == null) {
            try { cachedPrompt = new ClassPathResource("agent/prompts/extract.txt").getContentAsString(StandardCharsets.UTF_8); }
            catch (IOException e) { return "你是系统的长期记忆提取者"; }
        }
        return cachedPrompt;
    }

    private static String vectorToString(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(v[i]);
        }
        return sb.append("]").toString();
    }
}
