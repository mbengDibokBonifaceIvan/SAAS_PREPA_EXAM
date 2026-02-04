package com.ivan.backend.infrastructure.messaging;

import com.ivan.backend.domain.event.OrganizationRegisteredEvent;
import com.ivan.backend.domain.event.PasswordResetRequestedEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMQMessageAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQMessageAdapter adapter;

    @Test
    void should_send_json_organization_registered_event() {
        // Given: On prépare un événement conforme au record du Domain
        UUID orgId = UUID.randomUUID();
        var event = new OrganizationRegisteredEvent(
            orgId, 
            "Centre Test", 
            "ivan@test.com", 
            "Ivan", 
            "D",
            true
        );

        // When: On appelle l'adaptateur d'infrastructure
        adapter.publishOrganizationRegistered(event);

        // Then: On vérifie que le RabbitTemplate a reçu l'ordre d'envoi 
        // vers le bon Exchange et avec la bonne Routing Key
        verify(rabbitTemplate).convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY_ORG_REGISTERED,
            event
        );
    }

    @Test
    void should_send_json_password_reset_requested_event() {
        // GIVEN
        var event = new PasswordResetRequestedEvent("ivan@test.com", LocalDateTime.now());

        // WHEN
        adapter.publishPasswordResetRequested(event);

        // THEN
        verify(rabbitTemplate).convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY_PASSWORD_RESET_REQUESTED,
            event
        );
    }
    
}