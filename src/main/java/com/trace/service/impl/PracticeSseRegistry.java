package com.trace.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刷题判题 SSE 注册中心 —— 前端等待判题完成。
 */
@Slf4j
@Component
public class PracticeSseRegistry {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long recordId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        emitters.put(recordId, emitter);
        emitter.onCompletion(() -> emitters.remove(recordId));
        emitter.onTimeout(() -> emitters.remove(recordId));
        emitter.onError(e -> emitters.remove(recordId));
        return emitter;
    }

    public void pushCompleted(Long recordId, Map<String, Object> data) {
        SseEmitter emitter = emitters.get(recordId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("completed").data(data));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE推送失败: recordId={}, 错误位置=PracticeSseRegistry.pushCompleted", recordId, e);
                emitters.remove(recordId);
            }
        }
    }
}
