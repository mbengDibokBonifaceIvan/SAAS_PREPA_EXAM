package com.ivan.notification_service.application.dto;

import com.ivan.notification_service.domain.valueobject.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRequestTest {

    @Test
    @DisplayName("Constructor & Accessors : devrait stocker et retourner les valeurs correctement")
    void shouldCreateAndRetrieveValues() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        String recipient = "test@ivan.com";
        String title = "Promo";
        String message = "Offre limitée";
        NotificationType type = NotificationType.EMAIL;

        // WHEN
        NotificationRequest request = new NotificationRequest(userId, recipient, title, message, type);

        // THEN
        assertThat(request.userId()).isEqualTo(userId);
        assertThat(request.recipient()).isEqualTo(recipient);
        assertThat(request.title()).isEqualTo(title);
        assertThat(request.message()).isEqualTo(message);
        assertThat(request.type()).isEqualTo(type);
    }

    @Test
    @DisplayName("Equals & HashCode : deux records identiques devraient être égaux")
    void testEqualsAndHashCode() {
        UUID userId = UUID.randomUUID();
        
        NotificationRequest request1 = new NotificationRequest(userId, "a@b.com", "T", "M", NotificationType.PUSH);
        NotificationRequest request2 = new NotificationRequest(userId, "a@b.com", "T", "M", NotificationType.PUSH);
        NotificationRequest request3 = new NotificationRequest(UUID.randomUUID(), "other@b.com", "T", "M", NotificationType.PUSH);

        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).hasSameHashCodeAs(request2.hashCode());
        assertThat(request1).isNotEqualTo(request3);
    }

    @Test
    @DisplayName("ToString : devrait contenir tous les champs")
    void testToString() {
        NotificationRequest request = new NotificationRequest(UUID.randomUUID(), "a@b.com", "Hello", "World", NotificationType.EMAIL);
        
        assertThat(request.toString())
                .contains("userId")
                .contains("a@b.com")
                .contains("Hello")
                .contains("World");
    }
}