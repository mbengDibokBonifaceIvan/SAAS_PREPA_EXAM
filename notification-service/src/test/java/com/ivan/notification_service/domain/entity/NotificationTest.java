package com.ivan.notification_service.domain.entity;

import com.ivan.notification_service.domain.exception.InvalidNotificationException;
import com.ivan.notification_service.domain.valueobject.NotificationStatus;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Nested;

@ActiveProfiles("test")
class NotificationTest {

    private final UUID userId = UUID.randomUUID();
    private final String recipient = "test@ivan.com";
    private final String title = "Test Title";
    private final String message = "Test Message Content";

    @Nested
    @DisplayName("Tests de Création et Validation")
    class CreationTests {

        @Test
        @DisplayName("Devrait créer une notification valide avec le statut PENDING")
        void shouldCreateValidNotification() {
            Notification notification = createNotification();

            assertNotNull(notification.getId());
            assertEquals(userId, notification.getUserId());
            assertEquals(NotificationStatus.PENDING, notification.getStatus());
            assertNotNull(notification.getCreatedAt());
        }

        @Test
        @DisplayName("Devrait lever une exception si le User ID est manquant")
        void shouldFailWhenUserIdIsNull() {
            // Refactor : On prépare l'objet hors de la lambda
            Notification.NotificationBuilder builder = Notification.builder()
                    .userId(null)
                    .recipient(recipient)
                    .type(NotificationType.EMAIL);

            // Seul l'appel provoquant l'exception est dans la lambda
            InvalidNotificationException exception = assertThrows(
                InvalidNotificationException.class, 
                builder::build
            );
            
            assertEquals("userId", exception.getFieldName()); 
        }

        @Test
        @DisplayName("Devrait lever une exception si le destinataire est vide")
        void shouldFailWhenRecipientIsEmpty() {
            Notification.NotificationBuilder builder = Notification.builder()
                    .userId(userId)
                    .recipient("")
                    .type(NotificationType.EMAIL);

            assertThrows(InvalidNotificationException.class, builder::build);
        }

        @Test
        @DisplayName("Devrait utiliser l'ID fourni au lieu d'en générer un nouveau")
        void shouldUseProvidedId() {
            UUID fixedId = UUID.randomUUID();
            Notification notification = Notification.builder()
                    .id(fixedId)
                    .userId(userId)
                    .recipient(recipient)
                    .type(NotificationType.EMAIL)
                    .build();

            assertEquals(fixedId, notification.getId());
        }
    }

    @Nested
    @DisplayName("Tests de changement d'état (Workflow)")
    class StatusTests {

        @Test
        @DisplayName("Devrait passer le statut à SENT")
        void shouldMarkAsSent() {
            Notification notification = createNotification();
            notification.markAsSent();
            assertEquals(NotificationStatus.SENT, notification.getStatus());
        }

        @Test
        @DisplayName("Devrait passer le statut à FAILED")
        void shouldMarkAsFailed() {
            Notification notification = createNotification();
            notification.markAsFailed();
            assertEquals(NotificationStatus.FAILED, notification.getStatus());
        }
    }

    private Notification createNotification() {
        return Notification.builder()
                .userId(userId)
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(NotificationType.EMAIL)
                .build();
    }
}