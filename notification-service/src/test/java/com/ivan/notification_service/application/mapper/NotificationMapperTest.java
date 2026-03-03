package com.ivan.notification_service.application.mapper;

import com.ivan.notification_service.application.dto.NotificationRequest;
import com.ivan.notification_service.application.dto.NotificationResponse;
import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.exception.InvalidNotificationException;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationMapperTest {

    private final NotificationMapper mapper = new NotificationMapper();

    @Test
    @DisplayName("toDomain : devrait transformer un Request en entité Notification")
    void shouldMapRequestToDomain() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        NotificationRequest request = new NotificationRequest(
                userId,
                "test@ivan.com",
                "Bienvenue",
                "Message de test",
                NotificationType.EMAIL);

        // WHEN
        Notification result = mapper.toDomain(request);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getRecipient()).isEqualTo("test@ivan.com");
        assertThat(result.getTitle()).isEqualTo("Bienvenue");
        assertThat(result.getMessage()).isEqualTo("Message de test");
        assertThat(result.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(result.getStatus().name()).isEqualTo("PENDING"); // Par défaut à la création
        assertThat(result.getId()).isNotNull(); // Vérifie que le constructeur génère un ID
    }

    @Test
    @DisplayName("toResponse : devrait transformer l'entité Notification en Response DTO")
    void shouldMapDomainToResponse() {
        // GIVEN
        UUID notifId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Notification domain = Notification.builder()
                .id(notifId)
                .userId(userId)
                .recipient("ivan@test.com")
                .title("Alerte")
                .message("Test message")
                .type(NotificationType.PUSH)
                .build();

        domain.markAsSent(); // On change le statut pour vérifier le mapping de l'Enum

        // WHEN
        NotificationResponse result = mapper.toResponse(domain);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(notifId);
        assertThat(result.title()).isEqualTo("Alerte");
        assertThat(result.message()).isEqualTo("Test message");
        assertThat(result.status()).isEqualTo("SENT");
        assertThat(result.createdAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Validation : devrait échouer si userId est null")
    void shouldFailWhenUserIdIsNull() {
        NotificationRequest invalidRequest = new NotificationRequest(null, "r@r.com", "t", "m", NotificationType.EMAIL);

        assertThrows(
                InvalidNotificationException.class,
                () -> mapper.toDomain(invalidRequest));
    }
}