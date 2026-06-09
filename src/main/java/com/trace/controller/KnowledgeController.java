package com.trace.controller;

import com.trace.dto.ApiResponse;
import com.trace.dto.ChatRequest;
import com.trace.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /**
     * 知识科普 - SSE 流式聊天
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@AuthenticationPrincipal Long userId,
                              @Valid @RequestBody ChatRequest request) {
        return knowledgeService.chatStream(userId, request.getMessage(), request.getDomain());
    }
}
