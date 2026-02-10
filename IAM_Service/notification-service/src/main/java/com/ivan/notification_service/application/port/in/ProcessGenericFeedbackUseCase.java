package com.ivan.notification_service.application.port.in;

import java.util.UUID;

import com.ivan.notification_service.domain.valueobject.NotificationType;

// 3. Feedback Générique (Actions UI)
public interface ProcessGenericFeedbackUseCase {
    void handle(UUID userId, String recipient, String title, String body, NotificationType type);
}