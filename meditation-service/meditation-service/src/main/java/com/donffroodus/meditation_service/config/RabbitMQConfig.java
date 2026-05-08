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
        ObjectMapper objectMapper = new ObjectMapper();
        
        objectMapper.registerModule(new JavaTimeModule());
        
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
