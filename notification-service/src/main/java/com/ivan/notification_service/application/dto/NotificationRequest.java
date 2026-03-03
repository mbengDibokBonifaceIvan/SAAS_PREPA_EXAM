package com.ivan.notification_service.application.dto;

import com.ivan.notification_service.domain.valueobject.NotificationType;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record NotificationRequest(
    @Schema(example = "123e4567-e89b-12d3-a456-426614174000") UUID userId,
    @Schema(example = "user@example.com") String recipient,
    @Schema(example = "Bonjour !") String title,
    @Schema(example = "Ceci est un test") String message,
    @Schema(example = "EMAIL") NotificationType type
) {}