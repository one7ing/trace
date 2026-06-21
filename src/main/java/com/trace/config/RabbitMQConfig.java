package com.trace.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PLAN_QUEUE = "trace.plan.generate";
    public static final String PLAN_EXCHANGE = "trace.plan.exchange";
    public static final String PLAN_ROUTING_KEY = "trace.plan.generate";

    public static final String INTERVIEW_EVAL_QUEUE = "trace.interview.eval";
    public static final String INTERVIEW_EVAL_EXCHANGE = "trace.interview.exchange";
    public static final String INTERVIEW_EVAL_ROUTING_KEY = "trace.interview.eval";

    @Bean
    public Queue planGenerateQueue() {
        return QueueBuilder.durable(PLAN_QUEUE).build();
    }

    @Bean
    public DirectExchange planExchange() {
        return new DirectExchange(PLAN_EXCHANGE);
    }

    @Bean
    public Binding planGenerateBinding(Queue planGenerateQueue, DirectExchange planExchange) {
        return BindingBuilder.bind(planGenerateQueue).to(planExchange).with(PLAN_ROUTING_KEY);
    }

    @Bean
    public Queue interviewEvalQueue() {
        return QueueBuilder.durable(INTERVIEW_EVAL_QUEUE).build();
    }

    @Bean
    public DirectExchange interviewEvalExchange() {
        return new DirectExchange(INTERVIEW_EVAL_EXCHANGE);
    }

    @Bean
    public Binding interviewEvalBinding(Queue interviewEvalQueue, DirectExchange interviewEvalExchange) {
        return BindingBuilder.bind(interviewEvalQueue).to(interviewEvalExchange)
                .with(INTERVIEW_EVAL_ROUTING_KEY);
    }

    // === 长期记忆异步提取 ===
    public static final String MEMORY_EXTRACT_QUEUE = "trace.memory.extract";
    public static final String MEMORY_EXTRACT_EXCHANGE = "trace.memory.exchange";
    public static final String MEMORY_EXTRACT_ROUTING_KEY = "trace.memory.extract";

    @Bean
    public Queue memoryExtractQueue() {
        return QueueBuilder.durable(MEMORY_EXTRACT_QUEUE).build();
    }

    @Bean
    public DirectExchange memoryExtractExchange() {
        return new DirectExchange(MEMORY_EXTRACT_EXCHANGE);
    }

    @Bean
    public Binding memoryExtractBinding(Queue memoryExtractQueue, DirectExchange memoryExtractExchange) {
        return BindingBuilder.bind(memoryExtractQueue).to(memoryExtractExchange)
                .with(MEMORY_EXTRACT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
