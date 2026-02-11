package com.ivan.notification_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import jakarta.validation.constraints.Email;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Requête pour les notifications standards (Bienvenue, Activation, Reset)")
public record UserNotificationRequest(
    @Schema(description = "Identifiant unique de l'utilisateur", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull UUID userId,

    @Schema(description = "Email de destination", example = "ivan.d@skilyo.com")
    @NotBlank @Email String email,

    @Schema(description = "Nom complet du destinataire pour personnalisation", example = "Ivan Dalton")
    @NotBlank String name,

    @Schema(description = "Information contextuelle variable (Nom d'organisation, Rôle, ou Motif)", 
            example = "Admin")
    String detail 
) {}