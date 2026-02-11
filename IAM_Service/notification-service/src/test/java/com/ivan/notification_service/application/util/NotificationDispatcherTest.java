package com.ivan.notification_service.application.util;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.port.out.NotificationRepository;
import com.ivan.notification_service.domain.port.out.NotificationSender;
import com.ivan.notification_service.domain.valueobject.NotificationStatus;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationSender emailSender;

    @Mock
    private NotificationSender pushSender;

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        // On simule l'injection de la liste des senders par Spring
        dispatcher = new NotificationDispatcher(repository, List.of(pushSender, emailSender));    }

    @Test
    @DisplayName("Succès : Devrait utiliser le bon sender et marquer comme SENT")
    void shouldDispatchSuccessfully() {
        // GIVEN
        Notification notification = createTestNotification(NotificationType.EMAIL);
        
        when(emailSender.supports(NotificationType.EMAIL)).thenReturn(true);
        when(pushSender.supports(NotificationType.EMAIL)).thenReturn(false);

        // WHEN
        dispatcher.dispatch(notification);

        // THEN
        verify(emailSender, times(1)).send(notification);
        verify(pushSender, never()).send(any());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        
        // Vérifie qu'on sauvegarde au début (PENDING) et à la fin (SENT)
        verify(repository, times(2)).save(notification);
    }

    @Test
    @DisplayName("Échec : Devrait marquer comme FAILED si le sender lève une exception")
    void shouldMarkAsFailedWhenSenderFails() {
        // GIVEN
        Notification notification = createTestNotification(NotificationType.PUSH);
        
        when(pushSender.supports(NotificationType.PUSH)).thenReturn(true);
        doThrow(new RuntimeException("Erreur réseau")).when(pushSender).send(notification);

        // WHEN
        dispatcher.dispatch(notification);

        // THEN
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        verify(repository, times(2)).save(notification);
    }

    @Test
    @DisplayName("Échec : Devrait marquer comme FAILED si aucun sender n'est trouvé")
    void shouldMarkAsFailedWhenNoSenderFound() {
        // GIVEN
        Notification notification = createTestNotification(NotificationType.EMAIL);
        when(emailSender.supports(any())).thenReturn(false);
        when(pushSender.supports(any())).thenReturn(false);

        // WHEN
        dispatcher.dispatch(notification);

        // THEN
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        verify(repository, times(2)).save(notification);
    }

    private Notification createTestNotification(NotificationType type) {
        // Utilisation du builder de ton entité
        return Notification.builder()
                .userId(java.util.UUID.randomUUID())
                .recipient("test@ivan.com")
                .title("Test")
                .message("Message")
                .type(type)
                .build();
    }
}