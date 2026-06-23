package com.trace.controller;

import com.trace.agent.AbstractAgent;
import com.trace.agent.KnowledgeAgent;
import com.trace.dto.ApiResponse;
import com.trace.dto.ChatRequest;
import com.trace.entity.ChatHistory;
import com.trace.service.MemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Tag(name = "AI 知识问答", description = "SSE 流式 AI 对话，支持联网搜索 / RAG 知识库 / 默认对话三种模式")
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeAgent knowledgeAgent;
    private final MemoryService memoryService;

    @Operation(summary = "AI 对话", description = "发送消息给 AI，SSE 流式返回。mode: direct(默认) / web(联网) / rag(知识库)")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@AuthenticationPrincipal Long userId,
                             @Valid @RequestBody ChatRequest request) {
        String mode = request.getMode() != null ? request.getMode() : "direct";
        return knowledgeAgent.handleStreamWithMode(
                request.getMessage(), userId, mode, request.getKnowledgeBaseTopic());
    }

    @Operation(summary = "停止回答", description = "取消当前用户正在进行的 AI 流式回答")
    @PostMapping("/stop")
    public ApiResponse<String> stop(@AuthenticationPrincipal Long userId) {
        AbstractAgent.cancel(userId);
        return ApiResponse.success("已发送停止信号");
    }

    @Operation(summary = "聊天历史", description = "查询聊天记录，支持游标分页上滑加载更多")
    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> history(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "20") int limit) {
        List<ChatHistory> chats = memoryService.getChatsBefore(userId, beforeId, limit);
        boolean hasMore = chats.size() == limit;
        return ApiResponse.success(Map.of(
                "records", chats,
                "hasMore", hasMore
        ));
    }
}
