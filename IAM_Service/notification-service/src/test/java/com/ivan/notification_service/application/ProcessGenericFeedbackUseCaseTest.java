package com.ivan.notification_service.application;

import com.ivan.notification_service.application.dto.FeedbackRequest;
import com.ivan.notification_service.application.usecase.ProcessGenericFeedbackUseCaseImpl;
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
class ProcessGenericFeedbackUseCaseTest {

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private ProcessGenericFeedbackUseCaseImpl useCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("Devrait formater le titre avec la sévérité et forcer le type PUSH")
    void shouldDispatchFormattedPushNotification() {
        // GIVEN
        String title = "Opération réussie";
        String message = "Les données ont été sauvegardées.";
        String severity = "success"; // Test en minuscule pour vérifier le toUpperCase()

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // WHEN
        useCase.handle(new FeedbackRequest(userId, title, message, severity));

        // THEN
        verify(dispatcher).dispatch(captor.capture());
        Notification notification = captor.getValue();

        // Vérification du formatage du titre
        assertEquals("[SUCCESS] Opération réussie", notification.getTitle());

        // Vérification de la cible et du type
        assertEquals("UI-DASHBOARD", notification.getRecipient());
        assertEquals(NotificationType.PUSH, notification.getType());

        // Vérification des données de base
        assertEquals(userId, notification.getUserId());
        assertEquals(message, notification.getMessage());
    }

    @Test
    @DisplayName("Devrait fonctionner correctement avec une sévérité ERROR")
    void shouldHandleErrorSeverity() {
        // GIVEN
        String title = "Échec";
        String severity = "error";
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // WHEN
        useCase.handle(new FeedbackRequest(userId, title, "Message d'erreur", severity));

        // THEN
        verify(dispatcher).dispatch(captor.capture());
        assertEquals("[ERROR] Échec", captor.getValue().getTitle());
    }
}
