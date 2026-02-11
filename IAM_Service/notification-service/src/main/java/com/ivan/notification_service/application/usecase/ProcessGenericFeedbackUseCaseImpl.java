package com.ivan.notification_service.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;
import com.ivan.notification_service.application.dto.FeedbackRequest;
import com.ivan.notification_service.application.port.in.ProcessGenericFeedbackUseCase;
import com.ivan.notification_service.application.util.NotificationDispatcher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessGenericFeedbackUseCaseImpl implements ProcessGenericFeedbackUseCase {
    private final NotificationDispatcher dispatcher;

    @Override
    @Transactional
    public void handle(FeedbackRequest request) {
        // Logique générique : on prépare l'objet pour le dispatcher
        // On peut ajouter un petit formattage par défaut si besoin
        String finalTitle = String.format("[%s] %s", request.severity().toUpperCase(), request.title());

        Notification notification = Notification.builder()
                .userId(request.userId())
                .recipient("UI-DASHBOARD") // Pour un push, pas de mail, on cible l'UI
                .title(finalTitle)
                .message(request.message())
                .type(NotificationType.PUSH) // On force le type PUSH ici
                .build();

        dispatcher.dispatch(notification);
    }
}