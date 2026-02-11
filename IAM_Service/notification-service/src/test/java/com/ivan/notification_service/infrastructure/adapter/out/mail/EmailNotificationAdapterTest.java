package com.ivan.notification_service.infrastructure.adapter.out.mail;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationAdapter adapter;

    @Test
    @DisplayName("Devrait construire et envoyer un SimpleMailMessage correct")
    void shouldSendEmailWithCorrectMapping() {
        // GIVEN
        Notification notification = Notification.builder()
                .userId(UUID.randomUUID())
                .recipient("user@test.com")
                .title("Bienvenue")
                .message("Contenu du mail")
                .type(NotificationType.EMAIL)
                .build();

        // On utilise un ArgumentCaptor pour inspecter le message créé en interne
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // WHEN
        adapter.send(notification);

        // THEN
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals("imbeng18@skilyo-saas.com", sentMessage.getFrom());
        assertArrayEquals(new String[]{"user@test.com"}, sentMessage.getTo());
        assertEquals("Bienvenue", sentMessage.getSubject());
        assertEquals("Contenu du mail", sentMessage.getText());
    }

    @Test
    @DisplayName("Devrait supporter uniquement le type EMAIL")
    void shouldSupportOnlyEmailType() {
        assertTrue(adapter.supports(NotificationType.EMAIL));
        assertFalse(adapter.supports(NotificationType.PUSH));
    }
}