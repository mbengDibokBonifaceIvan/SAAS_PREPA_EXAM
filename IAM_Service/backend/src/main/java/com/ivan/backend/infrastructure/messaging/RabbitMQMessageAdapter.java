package com.ivan.backend.infrastructure.messaging;

import com.ivan.backend.domain.event.AccountActivatedEvent;
import com.ivan.backend.domain.event.AccountBannedEvent;
import com.ivan.backend.domain.event.AccountLockedEvent;
import com.ivan.backend.domain.event.OrganizationRegisteredEvent;
import com.ivan.backend.domain.event.PasswordResetRequestedEvent;
import com.ivan.backend.domain.event.UserProvisionedEvent;
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
                event);
    }

    @Override
    public void publishAccountLocked(AccountLockedEvent event) {
        log.warn("Publication de l'événement de verrouillage pour: {}", event.email());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_USER_LOCKED,
                event);
    }

    @Override
    public void publishPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Envoi de l'événement de réinitialisation pour : {}", event.email());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_PASSWORD_RESET_REQUESTED,
                event);
    }

    @Override
    public void publishUserProvisioned(UserProvisionedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_USER_PROVISIONED,
                event);
        log.info("Événement UserProvisioned publié pour : {}", event.email());
    }

      @Override
    public void publishAccountActivated(AccountActivatedEvent event) {
        log.info("Publication de l'événement d'activation pour: {}", event.userEmail());
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY_ACCOUNT_ACTIVATED,
            event
        );
    }

    @Override
    public void publishAccountBanned(AccountBannedEvent event) {
        log.info("Publication de l'événement de bannissement pour: {}", event.userEmail());
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY_ACCOUNT_BANNED,
            event
        );
    }
}
