package com.ivan.notification_service.application;

import com.ivan.notification_service.application.usecase.SendSecurityNotificationUseCaseImpl;
import com.ivan.notification_service.application.util.NotificationDispatcher;
import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SendSecurityNotificationUseCaseTest {

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private SendSecurityNotificationUseCaseImpl useCase;

    private final UUID userId = UUID.randomUUID();
    private final String userEmail = "ivan@test.com";
    private final String userName = "Ivan";

    @Test
    @DisplayName("Devrait générer une alerte de BANissement avec l'emoji 🚫")
    void shouldHandleBannedAlert() {
        // GIVEN
        String alertType = "ACCOUNT_BANNED";
        String reason = "Violation des CGU";

        // ArgumentCaptor permet d'intercepter l'objet Notification créé à l'intérieur du Use Case
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // WHEN
        useCase.handle(userId, userEmail, userName, alertType, reason);

        // THEN
        verify(dispatcher).dispatch(captor.capture());
        Notification notification = captor.getValue();

        assertEquals("🚫 COMPTE SUSPENDU", notification.getTitle());
        assertTrue(notification.getMessage().contains("COMPTE SUSPENDU"));
        assertTrue(notification.getMessage().contains(reason));
        assertEquals(NotificationType.EMAIL, notification.getType());
        assertEquals(userEmail, notification.getRecipient());
    }

    @Test
    @DisplayName("Devrait générer une alerte de VERROUILLAGE avec l'emoji ⚠️")
    void shouldHandleLockedAlert() {
        // GIVEN
        String alertType = "USER_LOCKED";
        String reason = "Trop de tentatives échouées";
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // WHEN
        useCase.handle(userId, userEmail, userName, alertType, reason);

        // THEN
        verify(dispatcher).dispatch(captor.capture());
        Notification notification = captor.getValue();

        assertEquals("⚠️ COMPTE VERROUILLÉ", notification.getTitle());
        assertTrue(notification.getMessage().contains("📌 ACTION : COMPTE VERROUILLÉ"));
    }

    @Test
    @DisplayName("Devrait générer une alerte de RÉINITIALISATION avec l'emoji 🔑")
    void shouldHandlePasswordResetAlert() {
        // GIVEN
        String alertType = "RÉINITIALISATION";
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // WHEN
        useCase.handle(userId, userEmail, userName, alertType, "Demande utilisateur");

        // THEN
        verify(dispatcher).dispatch(captor.capture());
        Notification notification = captor.getValue();

        assertEquals("🔑 RÉINITIALISATION DE MOT DE PASSE", notification.getTitle());
    }

    @Test
    @DisplayName("Devrait utiliser une alerte par défaut pour les types inconnus")
    void shouldHandleDefaultAlert() {
        // GIVEN
        String alertType = "UNKNOWN_ALERT";
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // WHEN
        useCase.handle(userId, userEmail, userName, alertType, "Raison quelconque");

        // THEN
        verify(dispatcher).dispatch(captor.capture());
        Notification notification = captor.getValue();

        assertEquals("🔒 ALERTE DE SÉCURITÉ", notification.getTitle());
    }
}
