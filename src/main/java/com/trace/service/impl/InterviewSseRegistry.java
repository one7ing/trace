package com.trace.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 面试评价 SSE 注册中心 —— 管理 SseEmitter 连接。
 * 前端进入面试详情页后通过 SSE 等待评价结果，消费者完成后推送。
 */
@Slf4j
@Component
public class InterviewSseRegistry {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /** 注册一个 SSE 连接 */
    public SseEmitter register(Long recordId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 分钟超时
        emitters.put(recordId, emitter);
        emitter.onCompletion(() -> emitters.remove(recordId));
        emitter.onTimeout(() -> emitters.remove(recordId));
        emitter.onError(e -> emitters.remove(recordId));
        return emitter;
    }

    /** 推送评价完成结果 */
    public void pushCompleted(Long recordId, Map<String, Object> data) {
        SseEmitter emitter = emitters.get(recordId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("completed").data(data));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE push failed for recordId={}", recordId, e);
                emitters.remove(recordId);
            }
        }
    }
}
