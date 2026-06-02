package com.donffroodus.meditation_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {
    @Bean
    public Queue waitingQueue() {
        return QueueBuilder.durable("meditation.waiting.queue")
                .withArgument("x-dead-letter-exchange", "meditation.timeout.dlx")
                .withArgument("x-dead-letter-routing-key", "timeout.key")
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
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
