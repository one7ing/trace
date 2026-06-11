package com.trace.service;

import reactor.core.publisher.Flux;

public interface KnowledgeService {

    Flux<String> chatStream(Long userId, String message, String domain);

    void saveKnowledgeMemory(Long userId, String question, String summary);
}
