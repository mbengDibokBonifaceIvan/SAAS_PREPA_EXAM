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

    private final SendSecurityNotificationUseCase securityUseCase;
    private final OnboardingUseCase onboardingUseCase;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onMessage(Map<String, Object> message, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        log.info("Événement reçu via RabbitMQ avec routingKey: {}", routingKey);

        try {
            switch (routingKey) {
                case "organization.registered": {
                    String email = String.valueOf(message.get("ownerEmail"));
                    String fullName = String.format("%s %s", message.get("ownerFirstName"),
                            message.get("ownerLastName"));
                    String orgName = String.valueOf(message.get("organizationName"));

                    Object rawId = message.get("ownerId");
                    UUID ownerId = (rawId instanceof UUID) ? (UUID) rawId : UUID.fromString(rawId.toString());

                    onboardingUseCase.handleOrganizationWelcome(ownerId, email, fullName, orgName);
                    break;
                }

                case "user.locked", "account.banned": {
                    String userEmail = String.valueOf(message.get("userEmail"));
                    String reasonFromIAM = String.valueOf(message.get("reason"));
                    // Ajout du nom pour la sécurité aussi
                    String fullName = String.format("%s %s", message.get("firstName"), message.get("lastName"));

                    Object rawUserId = message.get("userId");
                    UUID targetUserId = (rawUserId instanceof UUID) ? (UUID) rawUserId
                            : UUID.fromString(rawUserId.toString());

                    securityUseCase.handle(targetUserId, userEmail, fullName, routingKey.toUpperCase(), reasonFromIAM);
                    break;
                }

                case "account.activated": {
                    String firstName = String.valueOf(message.get("firstName"));
                    String lastName = String.valueOf(message.get("lastName"));
                    String userEmail = String.valueOf(message.get("userEmail"));
                    String reason = String.valueOf(message.get("reason"));
                    String fullName = (message.get("firstName") != null) ? firstName + " " + lastName
                            : "cher utilisateur";

                    Object rawId = message.get("userId");
                    UUID userId = (rawId instanceof UUID) ? (UUID) rawId : UUID.fromString(rawId.toString());

                    onboardingUseCase.handleAccountActivation(userId, userEmail, fullName,
                            "votre compte est désormais actif (" + reason + ")");
                    break;
                }

                case "password.reset.requested": {
                    String email = String.valueOf(message.get("userEmail"));
                    String firstName = String.valueOf(message.get("firstName"));
                    String lastName = String.valueOf(message.get("lastName"));
                    String fullName = (firstName != null) ? firstName + " " + lastName : "Cher utilisateur";

                    Object rawId = message.get("userId");
                    UUID userId = (rawId instanceof UUID) ? (UUID) rawId : UUID.fromString(rawId.toString());

                    // On utilise le securityUseCase car c'est une action sensible
                    securityUseCase.handle(
                            userId,
                            email,
                            fullName,
                            "RÉINITIALISATION DE MOT DE PASSE",
                            "Une demande de modification de vos identifiants a été reçue.");
                    break;
                }

                case "user.provisioned": {
                    String email = String.valueOf(message.get("userEmail"));
                    String fullName = (message.get("firstName") != null)
                            ? message.get("firstName") + " " + message.get("lastName")
                            : "Cher collaborateur";
                    String role = String.valueOf(message.get("role"));

                    Object rawId = message.get("userId");
                    UUID userId = (rawId instanceof UUID) ? (UUID) rawId : UUID.fromString(rawId.toString());

                    onboardingUseCase.handleUserProvisioned(userId, email, fullName, role);
                    break;
                }
                default:
                    log.debug("Routing key non traitée : {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement du message RabbitMQ", e);
            // En relançant l'exception, Spring AMQP n'envoie pas l'ACK (Acknowledgment)
            // Le message sera re-livré selon la politique de ton broker.
            throw e;
        }
    }
}