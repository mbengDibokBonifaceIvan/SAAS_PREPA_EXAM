package com.ivan.notification_service.application.dto;

import com.ivan.notification_service.domain.valueobject.NotificationType;
import java.util.UUID;

public record NotificationRequest(
    UUID userId,
    String recipient,
    String title,
    String message,
    NotificationType type
) {}