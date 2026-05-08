package com.donffroodus.meditation_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitMQConfig {
    @Bean
    public Queue waitingQueue() {
        return QueueBuilder.durable("meditation.waiting.queue")
        .withArgument("x-dead-letter-exchange", "meditation.timeout.dlx") // 死了去哪
        .withArgument("x-dead-letter-routing-key", "timeout.key")        // 去了用啥 Key
        .build();
    }
    
    @Bean
    public TopicExchange waitingExchange() {
        return new TopicExchange("meditation.wait.exchange");
    }
    
    @Bean
    public Binding waitingBinding(Queue waitingQueue, TopicExchange waitingExchange) {
        return BindingBuilder.bind(waitingQueue).to(waitingExchange).with("wait.key");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // 1. 自己 new 一个 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 2. 亲手给它装上 Java 8 时间模块（解决 LocalDateTime 报错的核心）
        objectMapper.registerModule(new JavaTimeModule());
        
        // 3. 强烈建议加这行：让时间变成 "2026-05-08T10:30:00" 这种人类可读的字符串，
        // 而不是变成毫无意义的时间戳数组 [2026, 5, 8, 10, 30, 0]
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 4. 将组装好的装填进 RabbitMQ 转换器
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
