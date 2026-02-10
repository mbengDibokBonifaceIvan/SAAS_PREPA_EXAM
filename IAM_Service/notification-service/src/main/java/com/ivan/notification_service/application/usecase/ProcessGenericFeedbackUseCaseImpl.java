package com.ivan.notification_service.application.usecase;


import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.notification_service.domain.entity.Notification;
import com.ivan.notification_service.domain.valueobject.NotificationType;

import lombok.RequiredArgsConstructor;

import com.ivan.notification_service.application.port.in.ProcessGenericFeedbackUseCase;
import com.ivan.notification_service.application.util.NotificationDispatcher;


@Service
@RequiredArgsConstructor
public class ProcessGenericFeedbackUseCaseImpl implements ProcessGenericFeedbackUseCase {
    private final NotificationDispatcher dispatcher;

    @Override
    @Transactional
    public void handle(UUID userId, String recipient, String title, String body, NotificationType type) {
        Notification notification = Notification.builder()
            .userId(userId)
            .recipient(recipient)
            .title(title)
            .message(body)
            .type(type)
            .build();
        dispatcher.dispatch(notification);
    }
}