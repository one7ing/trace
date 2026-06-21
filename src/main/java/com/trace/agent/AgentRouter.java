package com.trace.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 意图分析路由器：根据用户输入选择合适的 Agent 执行任务
 */
@Slf4j
@Component
public class AgentRouter {

    private final List<Agent> agents;
    private final Agent defaultAgent;

    public AgentRouter(List<Agent> agents) {
        this.agents = agents;
        // KnowledgeAgent 作为默认兜底
        this.defaultAgent = agents.stream()
                .filter(a -> "knowledge".equals(a.name()))
                .findFirst()
                .orElse(agents.get(0));
    }

    /**
     * 根据意图路由到对应 Agent
     */
    public Agent route(String userInput, Long userId) {
        for (Agent agent : agents) {
            if (!"knowledge".equals(agent.name()) && agent.canHandle(userInput, userId)) {
                log.info("AgentRouter: routed to {} for userId={}", agent.name(), userId);
                return agent;
            }
        }
        return defaultAgent;
    }

    /**
     * 流式路由处理
     */
    public Flux<String> handleStream(String userInput, Long userId) {
        Agent agent = route(userInput, userId);
        return agent.handleStream(userInput, userId);
    }

    /**
     * 取消指定用户的流式生成 —— 委托给 AbstractAgent 的 Redis 取消信号。
     */
    public void cancel(Long userId) {
        AbstractAgent.cancel(userId);
    }

    /** 获取所有已注册的 Agent（用于调试） */
    public List<String> listAgents() {
        return agents.stream().map(Agent::name).toList();
    }
}
