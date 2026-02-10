package com.ivan.notification_service.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // On reprend scrupuleusement le nom de l'exchange de l'IAM
    public static final String IAM_EXCHANGE = "iam.exchange";
    
    // On crée une file d'attente spécifique pour ce service
    public static final String NOTIFICATION_QUEUE = "q.notification.all";

    @Bean
    public JacksonJsonMessageConverter consumerJackson2MessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public TopicExchange iamExchange() {
        return new TopicExchange(IAM_EXCHANGE);
    }

    // On lie la Queue à l'Exchange pour TOUS les événements liés aux utilisateurs et organisations
    // Le pattern "#" signifie "tout ce qui suit"
    @Bean
    public Binding binding() {
        return BindingBuilder.bind(notificationQueue())
                .to(iamExchange())
                .with("#"); // On écoute tout, on triera dans le listener
    }
}