package com.ivan.notification_service.application.usecase;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import com.ivan.notification_service.application.port.in.ProcessGenericFeedbackUseCase;
import com.ivan.notification_service.application.util.NotificationDispatcher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessGenericFeedbackUseCaseImpl implements ProcessGenericFeedbackUseCase {
    private final NotificationDispatcher dispatcher;

    @Override
    @Transactional
    public void handle(UUID userId, String title, String message, String severity) {
        // Logique générique : on prépare l'objet pour le dispatcher
        // On peut ajouter un petit formattage par défaut si besoin
        String finalTitle = String.format("[%s] %s", severity.toUpperCase(), title);

        Notification notification = Notification.builder()
                .userId(userId)
                .recipient("UI-DASHBOARD") // Pour un push, pas de mail, on cible l'UI
                .title(finalTitle)
                .message(message)
                .type(NotificationType.PUSH) // On force le type PUSH ici
                .build();

        dispatcher.dispatch(notification);
    }
}