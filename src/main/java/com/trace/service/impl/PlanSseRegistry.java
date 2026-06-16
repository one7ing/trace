package com.trace.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 计划生成 SSE 注册中心 —— 管理 SseEmitter 连接。
 * 前端提交计划后通过 SSE 等待结果，消费者完成后推送。
 */
@Slf4j
@Component
public class PlanSseRegistry {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /** 注册一个 SSE 连接 */
    public SseEmitter register(Long planId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 分钟超时，给 AI 生成留足时间
        emitters.put(planId, emitter);
        emitter.onCompletion(() -> emitters.remove(planId));
        emitter.onTimeout(() -> emitters.remove(planId));
        emitter.onError(e -> emitters.remove(planId));
        return emitter;
    }

    /** 推送完成结果 */
    public void pushCompleted(Long planId, Map<String, Object> data) {
        SseEmitter emitter = emitters.get(planId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("completed").data(data));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE push failed for planId={}", planId, e);
                emitters.remove(planId);
            }
        }
    }
}
