package com.ivan.notification_service.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;

import lombok.RequiredArgsConstructor;

import com.ivan.notification_service.application.port.in.SendSecurityNotificationUseCase;
import com.ivan.notification_service.application.util.NotificationDispatcher;

@Service
@RequiredArgsConstructor
public class SendSecurityNotificationUseCaseImpl implements SendSecurityNotificationUseCase {
    private final NotificationDispatcher dispatcher;

    @Override
    @Transactional
    public void handle(UUID userId, String email, String reason) {
        Notification notification = Notification.builder()
            .userId(userId)
            .recipient(email)
            .title("ALERTE SÉCURITÉ")
            .message("Votre compte a été restreint pour la raison suivante : " + reason)
            .type(NotificationType.EMAIL)
            .build();

        dispatcher.dispatch(notification);
    }
}
