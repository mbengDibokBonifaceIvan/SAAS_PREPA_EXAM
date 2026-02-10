package com.ivan.notification_service.infrastructure.adapter.in.messaging;

import com.ivan.notification_service.application.port.in.SendOnboardingWelcomeUseCase;
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
    private final SendOnboardingWelcomeUseCase onboardingUseCase;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onMessage(Map<String, Object> message, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        log.info("Événement reçu via RabbitMQ avec routingKey: {}", routingKey);

        try {
            switch (routingKey) {
                case "organization.registered":
                    onboardingUseCase.handle(
                        UUID.fromString((String) message.get("adminId")),
                        (String) message.get("adminEmail"),
                        (String) message.get("organizationName")
                    );
                    break;

                case "user.locked","account.banned":
                    securityUseCase.handle(
                        UUID.fromString((String) message.get("userId")),
                        (String) message.get("email"),
                        routingKey.replace(".", " ").toUpperCase()
                    );
                    break;

                // Ajoute les autres cas ici...
                default:
                    log.debug("Routing key non traitée : {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement du message RabbitMQ", e);
            // Ici, on pourrait lever une exception pour que RabbitMQ retente l'envoi
        }
    }
}