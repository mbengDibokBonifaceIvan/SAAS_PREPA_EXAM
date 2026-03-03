package com.ivan.notification_service.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationResponse(
    @Schema(description = "ID unique de la notification") UUID id,
    @Schema(description = "Titre de l'alerte", example = "✅ Votre compte est prêt") String title,
    @Schema(description = "Contenu du message") String message,
    @Schema(description = "Statut de l'envoi", example = "SENT") String status,
    @Schema(description = "Date de création") LocalDateTime createdAt
) {}