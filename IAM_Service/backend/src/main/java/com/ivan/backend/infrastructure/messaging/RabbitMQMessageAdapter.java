package com.ivan.backend.infrastructure.messaging;

import com.ivan.backend.domain.event.AccountLockedEvent;
import com.ivan.backend.domain.event.OrganizationRegisteredEvent;
import com.ivan.backend.domain.port.MessagePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQMessageAdapter implements MessagePublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOrganizationRegistered(OrganizationRegisteredEvent event) {
        log.info("Publication de l'événement d'inscription pour l'organisation: {}", event.organizationName());
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY_ORG_REGISTERED,
            event
        );
    }

    @Override
public void publishAccountLocked(AccountLockedEvent event) {
    log.warn("Publication de l'événement de verrouillage pour: {}", event.email());
    
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE_NAME,
        RabbitMQConfig.ROUTING_KEY_USER_LOCKED,
        event
    );
}
}