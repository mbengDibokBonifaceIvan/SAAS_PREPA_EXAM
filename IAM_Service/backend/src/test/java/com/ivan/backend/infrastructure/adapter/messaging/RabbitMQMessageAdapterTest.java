package com.ivan.backend.infrastructure.adapter.messaging;

import com.ivan.backend.domain.event.AccountActivatedEvent;
import com.ivan.backend.domain.event.AccountBannedEvent;
import com.ivan.backend.domain.event.AccountLockedEvent;
import com.ivan.backend.domain.event.OrganizationRegisteredEvent;
import com.ivan.backend.domain.event.PasswordResetRequestedEvent;
import com.ivan.backend.domain.event.UserProvisionedEvent;
import com.ivan.backend.infrastructure.config.RabbitMQConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RabbitMQMessageAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQMessageAdapter messageAdapter;

    @Test
    void should_send_json_organization_registered_event() {
        // Given: On prépare un événement conforme au record du Domain
        UUID orgId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        var event = new OrganizationRegisteredEvent(
                orgId,
                "Centre Test",
                ownerId,
                "ivan@test.com",
                "Ivan",
                "D",
                true);

        // When: On appelle l'adaptateur d'infrastructure
        messageAdapter.publishOrganizationRegistered(event);

        // Then: On vérifie que le RabbitTemplate a reçu l'ordre d'envoi
        // vers le bon Exchange et avec la bonne Routing Key
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_ORG_REGISTERED,
                event);
    }

    @Test
    void should_send_json_password_reset_requested_event() {
        // GIVEN
        var event = new PasswordResetRequestedEvent(UUID.randomUUID(), "Ivan", "D", "ivan@test.com", LocalDateTime.now());

        // WHEN
        messageAdapter.publishPasswordResetRequested(event);

        // THEN
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_PASSWORD_RESET_REQUESTED,
                event);
    }

    @Test
    @DisplayName("Devrait envoyer l'événement d'organisation avec les bons paramètres")
    void shouldPublishOrganizationRegistered() {
        // GIVEN
        OrganizationRegisteredEvent event = new OrganizationRegisteredEvent(
                UUID.randomUUID(), "DevCorp", UUID.randomUUID(), "owner@test.com", "Ivan", "MBENG", true);

        // WHEN
        messageAdapter.publishOrganizationRegistered(event);

        // THEN
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY_ORG_REGISTERED),
                eq(event));
    }

    @Test
    @DisplayName("Sécurité : ne devrait pas propager d'exception si RabbitMQ échoue sur l'onboarding")
    void shouldNotThrowException_WhenRabbitMQIsDownDuringOnboarding() {
        // GIVEN
        OrganizationRegisteredEvent event = mock(OrganizationRegisteredEvent.class);
        doThrow(new RuntimeException("RabbitMQ Connection Refused"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // WHEN & THEN
        // On vérifie que le try/catch interne de l'adaptateur fonctionne
        assertDoesNotThrow(() -> messageAdapter.publishOrganizationRegistered(event));
    }

    @Test
    @DisplayName("Devrait envoyer l'événement de provisionnement utilisateur")
    void shouldPublishUserProvisioned() {
        // GIVEN
        UserProvisionedEvent event = new UserProvisionedEvent(
                UUID.randomUUID(),"John", "Doe", "new@test.com", null, UUID.randomUUID(), LocalDateTime.now());

        // WHEN
        messageAdapter.publishUserProvisioned(event);

        // THEN
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY_USER_PROVISIONED),
                eq(event));
    }

    @Test
    @DisplayName("Devrait envoyer l'événement de bannissement de compte")
    void shouldPublishAccountBanned() {
        // GIVEN
        var event = new AccountBannedEvent(UUID.randomUUID(),"John", "Doe", "banned@test.com", "Raison du bannissement", "owner@test.com",
                LocalDateTime.now());

        // WHEN
        messageAdapter.publishAccountBanned(event);

        // THEN
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY_ACCOUNT_BANNED),
                eq(event));
    }

    @Test
    @DisplayName("Devrait envoyer l'événement d'activation de compte")
    void shouldPublishAccountActivated() {
        // GIVEN
        var event = new AccountActivatedEvent(UUID.randomUUID(),"John", "Doe", "active@test.com", "Raison de l'activation", "owner@test.com",
                LocalDateTime.now());

        // WHEN
        messageAdapter.publishAccountActivated(event);

        // THEN
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY_ACCOUNT_ACTIVATED),
                eq(event));
    }

    @Test
    @DisplayName("Devrait envoyer l'événement de verrouillage de compte")
    void shouldPublishAccountLocked() {
        // GIVEN
        var event = new AccountLockedEvent(UUID.randomUUID(), "John", "Doe", "locked@test.com", "Raison du verrouillage", LocalDateTime.now(),
                "ownerEmail@test.com");

        // WHEN
        messageAdapter.publishAccountLocked(event);

        // THEN
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY_USER_LOCKED),
                eq(event));
    }
}