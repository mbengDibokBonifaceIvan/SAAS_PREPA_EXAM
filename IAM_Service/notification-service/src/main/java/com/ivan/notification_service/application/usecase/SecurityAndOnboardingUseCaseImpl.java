package com.ivan.notification_service.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;

import lombok.RequiredArgsConstructor;

import com.ivan.notification_service.application.port.in.SendOnboardingWelcomeUseCase;
import com.ivan.notification_service.application.util.NotificationDispatcher;


@Service
@RequiredArgsConstructor
public class SecurityAndOnboardingUseCaseImpl implements  SendOnboardingWelcomeUseCase {
    private final NotificationDispatcher dispatcher;

    @Override
    @Transactional
    public void handle(UUID userId, String email, String name) {
        Notification notification = Notification.builder()
            .userId(userId)
            .recipient(email)
            .title("Bienvenue sur notre SAAS")
            .message("Bonjour " + name + ", votre compte est prêt !")
            .type(NotificationType.EMAIL)
            .build();
        dispatcher.dispatch(notification);
    }
}