package com.ivan.backend.infrastructure.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "iam.exchange";
    public static final String ROUTING_KEY_ORG_REGISTERED = "organization.registered";
    public static final String ROUTING_KEY_USER_LOCKED = "user.locked";
    public static final String ROUTING_KEY_PASSWORD_RESET_REQUESTED = "password.reset.requested";
    public static final String ROUTING_KEY_USER_PROVISIONED = "user.provisioned";
    public static final String ROUTING_KEY_ACCOUNT_ACTIVATED = "account.activated";
    public static final String ROUTING_KEY_ACCOUNT_BANNED = "account.banned";
    
    @Bean
    public JacksonJsonMessageConverter producerJackson2MessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public TopicExchange iamExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }
}