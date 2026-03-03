package com.ivan.notification_service.infrastructure.adapter.in.messaging;

import com.ivan.notification_service.application.port.in.OnboardingUseCase;
import com.ivan.notification_service.application.port.in.SendSecurityNotificationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMQNotificationListenerTest {

    @Mock
    private SendSecurityNotificationUseCase securityUseCase;

    @Mock
    private OnboardingUseCase onboardingUseCase;

    @InjectMocks
    private RabbitMQNotificationListener listener;

    private final UUID userId = UUID.randomUUID();
    private final String userEmail = "test@ivan.com";

    @Test
    @DisplayName("Devrait router 'organization.registered' vers handleOrganizationWelcome")
    void shouldHandleOrganizationRegistered() {
        // GIVEN
        Map<String, Object> message = new HashMap<>();
        message.put("ownerId", userId.toString());
        message.put("ownerEmail", userEmail);
        message.put("ownerFirstName", "Ivan");
        message.put("ownerLastName", "Dev");
        message.put("organizationName", "Ivan Corp");

        String routingKey = "organization.registered";

        // WHEN
        listener.onMessage(message, routingKey);

        // THEN
        verify(onboardingUseCase).handleOrganizationWelcome(
                eq(userId),
                eq(userEmail),
                eq("Ivan Dev"),
                eq("Ivan Corp")
        );
    }

    @Test
    @DisplayName("Devrait router 'user.locked' vers securityUseCase")
    void shouldHandleUserLocked() {
        // GIVEN
        Map<String, Object> message = new HashMap<>();
        message.put("userId", userId); // Test avec l'objet UUID directement
        message.put("userEmail", userEmail);
        message.put("firstName", "John");
        message.put("lastName", "Doe");
        message.put("reason", "Brute force detected");

        String routingKey = "user.locked";

        // WHEN
        listener.onMessage(message, routingKey);

        // THEN
        verify(securityUseCase).handle(
                eq(userId),
                eq(userEmail),
                eq("John Doe"),
                eq("USER.LOCKED"),
                eq("Brute force detected")
        );
    }

    @Test
    @DisplayName("Devrait router 'account.activated' vers handleAccountActivation")
    void shouldHandleAccountActivated() {
        // GIVEN
        Map<String, Object> message = new HashMap<>();
        message.put("userId", userId.toString());
        message.put("userEmail", userEmail);
        message.put("firstName", "Alice");
        message.put("lastName", "Wonder");
        message.put("reason", "Email verified");

        String routingKey = "account.activated";

        // WHEN
        listener.onMessage(message, routingKey);

        // THEN
        verify(onboardingUseCase).handleAccountActivation(
                eq(userId),
                eq(userEmail),
                eq("Alice Wonder"),
                org.mockito.ArgumentMatchers.contains("Email verified")
        );
    }

    @Test
    @DisplayName("Devrait gérer les noms manquants par une valeur par défaut")
    void shouldHandleMissingNamesGracefully() {
        // GIVEN
        Map<String, Object> message = new HashMap<>();
        message.put("userId", userId.toString());
        message.put("userEmail", userEmail);
        // Prénom et nom manquants

        String routingKey = "password.reset.requested";

        // WHEN
        listener.onMessage(message, routingKey);

        // THEN
        verify(securityUseCase).handle(
                eq(userId),
                eq(userEmail),
                eq("Cher utilisateur"), // Valeur par défaut attendue
                eq("RÉINITIALISATION DE MOT DE PASSE"),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}