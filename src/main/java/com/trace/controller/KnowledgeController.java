package com.trace.controller;

import com.trace.agent.AgentRouter;
import com.trace.dto.ApiResponse;
import com.trace.dto.ChatRequest;
import com.trace.entity.ChatHistory;
import com.trace.service.KnowledgeService;
import com.trace.service.MemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final AgentRouter agentRouter;
    private final MemoryService memoryService;

    /**
     * 知识科普 - SSE 流式聊天（通过 AgentRouter 路由）。
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@AuthenticationPrincipal Long userId,
                             @Valid @RequestBody ChatRequest request) {
        return agentRouter.handleStream(request.getMessage(), userId);
    }

    /**
     * 停止当前用户的流式生成。
     */
    @PostMapping("/stop")
    public ApiResponse<String> stop(@AuthenticationPrincipal Long userId) {
        agentRouter.cancel(userId);
        return ApiResponse.success("已发送停止信号");
    }

    /**
     * 查询聊天历史 —— 支持上滑分页加载更多。
     *
     * @param userId   当前用户
     * @param beforeId 分页游标（传此参数时查询比该 ID 更早的记录），不传则取最新
     * @param limit    每页条数，默认 20
     */
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
