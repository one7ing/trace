package com.trace.agent;

/**
 * Agent 核心接口 —— 所有路由 Agent 的统一契约
 */
public interface Agent {

    /** Agent 名称，用于路由匹配 */
    String name();

    /** 是否能处理当前输入 */
    boolean canHandle(String userInput, Long userId);

    /** 流式处理用户输入 */
    reactor.core.publisher.Flux<String> handleStream(String userInput, Long userId);

    /** 非流式处理用户输入 */
    String handle(String userInput, Long userId);
}
