package com.ivan.backend.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "iam.exchange";
    public static final String ROUTING_KEY_ORG_REGISTERED = "organization.registered";
    public static final String ROUTING_KEY_USER_LOCKED = "user.locked";
    
    // Indispensable pour envoyer des objets Java en JSON
    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange iamExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }
}