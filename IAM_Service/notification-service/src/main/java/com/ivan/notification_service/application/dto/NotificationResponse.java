package com.ivan.notification_service.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String title,
    String message,
    String status,
    LocalDateTime createdAt
) {}