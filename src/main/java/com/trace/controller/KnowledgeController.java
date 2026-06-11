package com.trace.controller;

import com.trace.agent.AgentRouter;
import com.trace.dto.ApiResponse;
import com.trace.dto.ChatRequest;
import com.trace.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final AgentRouter agentRouter;

    /**
     * 知识科普 - SSE 流式聊天（通过 AgentRouter 路由）
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@AuthenticationPrincipal Long userId,
                             @Valid @RequestBody ChatRequest request) {
        // 走路由 Agent 工作流
        return agentRouter.handleStream(request.getMessage(), userId);
    }

    /**
     * 停止当前用户的流式生成
     */
    @PostMapping("/stop")
    public ApiResponse<String> stop(@AuthenticationPrincipal Long userId) {
        agentRouter.cancel(userId);
        return ApiResponse.success("已发送停止信号");
    }
}
