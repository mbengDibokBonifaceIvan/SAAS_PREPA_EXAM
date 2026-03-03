package com.ivan.notification_service.infrastructure.adapter.in.messaging;

import com.ivan.notification_service.application.port.in.OnboardingUseCase;
import com.ivan.notification_service.application.port.in.SendSecurityNotificationUseCase;
import com.ivan.notification_service.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQNotificationListener {

    // Constantes pour éviter la duplication des littéraux
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_REASON = "reason";

    private final SendSecurityNotificationUseCase securityUseCase;
    private final OnboardingUseCase onboardingUseCase;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onMessage(Map<String, Object> message, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        log.info("Événement reçu via RabbitMQ avec routingKey: {}", routingKey);

        try {
            processRouting(message, routingKey);
        } catch (Exception e) {
            log.error("Erreur lors du traitement du message RabbitMQ", e);
            throw e;
        }
    }

    private void processRouting(Map<String, Object> message, String routingKey) {
        switch (routingKey) {
            case "organization.registered" -> handleOrgRegistered(message);
            case "user.locked", "account.banned" -> handleSecurityAlert(message, routingKey);
            case "account.activated" -> handleAccountActivated(message);
            case "password.reset.requested" -> handlePasswordReset(message);
            case "user.provisioned" -> handleUserProvisioning(message);
            default -> log.debug("Routing key non traitée : {}", routingKey);
        }
    }

    private void handleOrgRegistered(Map<String, Object> message) {
        String email = String.valueOf(message.get("ownerEmail"));
        String fullName = formatFullName(message.get("ownerFirstName"), message.get("ownerLastName"));
        String orgName = String.valueOf(message.get("organizationName"));
        UUID ownerId = parseUUID(message.get("ownerId"));

        onboardingUseCase.handleOrganizationWelcome(ownerId, email, fullName, orgName);
    }

    private void handleSecurityAlert(Map<String, Object> message, String routingKey) {
        String userEmail = String.valueOf(message.get(KEY_USER_EMAIL));
        String reason = String.valueOf(message.get(KEY_REASON));
        String fullName = formatFullName(message.get(KEY_FIRST_NAME), message.get(KEY_LAST_NAME));
        UUID targetUserId = parseUUID(message.get(KEY_USER_ID));

        securityUseCase.handle(targetUserId, userEmail, fullName, routingKey.toUpperCase(), reason);
    }

    private void handleAccountActivated(Map<String, Object> message) {
        String userEmail = String.valueOf(message.get(KEY_USER_EMAIL));
        String reason = String.valueOf(message.get(KEY_REASON));
        String fullName = formatFullName(message.get(KEY_FIRST_NAME), message.get(KEY_LAST_NAME));
        UUID userId = parseUUID(message.get(KEY_USER_ID));

        onboardingUseCase.handleAccountActivation(userId, userEmail, fullName,
                "votre compte est désormais actif (" + reason + ")");
    }

    private void handlePasswordReset(Map<String, Object> message) {
        String email = String.valueOf(message.get(KEY_USER_EMAIL));
        String fullName = formatFullName(message.get(KEY_FIRST_NAME), message.get(KEY_LAST_NAME));
        UUID userId = parseUUID(message.get(KEY_USER_ID));

        securityUseCase.handle(userId, email, fullName, "RÉINITIALISATION DE MOT DE PASSE",
                "Une demande de modification de vos identifiants a été reçue.");
    }

    private void handleUserProvisioning(Map<String, Object> message) {
        String email = String.valueOf(message.get(KEY_USER_EMAIL));
        String fullName = formatFullName(message.get(KEY_FIRST_NAME), message.get(KEY_LAST_NAME));
        String role = String.valueOf(message.get("role"));
        UUID userId = parseUUID(message.get(KEY_USER_ID));

        onboardingUseCase.handleUserProvisioned(userId, email, fullName, role);
    }

    // --- Utility Methods ---

    private String formatFullName(Object firstName, Object lastName) {
        if (firstName == null) return "Cher utilisateur";
        return String.format("%s %s", firstName, lastName);
    }

    private UUID parseUUID(Object rawId) {
        if (rawId instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(rawId));
    }
}