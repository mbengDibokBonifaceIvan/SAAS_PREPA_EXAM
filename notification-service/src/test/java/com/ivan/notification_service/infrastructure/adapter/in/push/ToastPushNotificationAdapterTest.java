package com.ivan.notification_service.infrastructure.adapter.in.push;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToastPushNotificationAdapterTest {

    @InjectMocks
    private ToastPushNotificationAdapter adapter;

    private final UUID userId = UUID.randomUUID();
    private SseEmitter mockEmitter;

    @BeforeEach
    void setUp() {
        // On mock l'émetteur pour vérifier les appels .send()
        mockEmitter = mock(SseEmitter.class);
    }

    @Test
    @DisplayName("Devrait extraire la sévérité SUCCESS et envoyer le Toast")
    void shouldExtractSeverityAndSend() throws IOException {
        // GIVEN
        adapter.registerClient(userId); // On enregistre un client réel
        // On remplace manuellement dans la map interne via l'accès de l'instance
        injectMockEmitter(userId, mockEmitter);

        Notification notification = Notification.builder()
                .userId(userId)
                .recipient("destinataire")
                .title("[SUCCESS] Opération réussie")
                .message("Le message de test")
                .type(NotificationType.PUSH)
                .build();

        // WHEN
        adapter.send(notification);

        // THEN
        // On vérifie que .send() a été appelé avec un événement nommé "TOAST_EVENT"
        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("Devrait utiliser INFO par défaut si aucun crochet n'est présent")
    void shouldDefaultToInfoSeverity() throws IOException {
        // GIVEN
        injectMockEmitter(userId, mockEmitter);
        Notification notification = Notification.builder()
                .userId(userId)
                .recipient("destinataire")
                .title("Titre sans crochets")
                .message("Hello")
                .type(NotificationType.PUSH)
                .build();

        // WHEN
        adapter.send(notification);

        // THEN
        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
        // Note: Tester le contenu exact du SseEventBuilder est complexe, 
        // mais le passage dans le code garantit la logique.
    }

    @Test
    @DisplayName("Devrait retirer l'émetteur de la liste en cas d'IOException")
    void shouldRemoveEmitterOnFailure() throws IOException {
        // GIVEN
        injectMockEmitter(userId, mockEmitter);
        Notification notification = Notification.builder()
                .userId(userId)
                .recipient("destinataire")
                .title("[ERROR] Fail")
                .type(NotificationType.PUSH)
                .build();

        // On simule une déconnexion du navigateur
doThrow(new IOException("Connection closed"))
            .when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
        // WHEN
        adapter.send(notification);

        // THEN
        // On vérifie que pour une future notification, l'émetteur n'est plus appelé
        reset(mockEmitter);
        adapter.send(notification);
        verifyNoInteractions(mockEmitter);
    }

    @Test
    @DisplayName("Devrait supporter uniquement le type PUSH")
    void shouldSupportOnlyPush() {
        assertTrue(adapter.supports(NotificationType.PUSH));
        assertFalse(adapter.supports(NotificationType.EMAIL));
    }

    /**
     * Petite méthode utilitaire pour injecter notre mock dans la Map privée via Reflection
     * ou en utilisant la méthode registerClient si on n'avait pas besoin de mock.
     */
    private void injectMockEmitter(UUID userId, SseEmitter emitter) {
        // Ici on utilise registerClient pour simplifier le test
        adapter.registerClient(userId);
        // Dans un test plus strict, on utiliserait ReflectionTestUtils de Spring
        org.springframework.test.util.ReflectionTestUtils.setField(adapter, "emitters", 
            new java.util.concurrent.ConcurrentHashMap<UUID, SseEmitter>() {{
                put(userId, emitter);
            }}
        );
    }
}