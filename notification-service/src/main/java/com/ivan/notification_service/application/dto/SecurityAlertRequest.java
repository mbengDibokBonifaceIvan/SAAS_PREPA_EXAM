package com.ivan.notification_service.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Requête pour les alertes de sécurité critiques")
public record SecurityAlertRequest(
    @Schema(description = "ID de l'utilisateur concerné")
    @NotNull UUID userId,

    @Schema(description = "Email d'alerte", example = "security@skilyo.com")
    @NotBlank @Email String email,

    @Schema(description = "Nom de l'utilisateur", example = "Ivan")
    @NotBlank String name,

    @Schema(description = "Type d'incident détecté", 
            example = "USER_LOCKED", 
            allowableValues = {"USER_LOCKED", "ACCOUNT_BANNED", "SUSPICIOUS_LOGIN"})
    @NotBlank String alertType,

    @Schema(description = "Description détaillée de la cause", 
            example = "Trop de tentatives de connexion échouées")
    @NotBlank String reason
) {}