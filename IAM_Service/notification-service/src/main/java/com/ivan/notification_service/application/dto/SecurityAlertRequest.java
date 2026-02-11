package com.ivan.notification_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import jakarta.validation.constraints.Email;

// DTO pour les alertes de sécurité complexes
public record SecurityAlertRequest(
    @NotNull UUID userId,
    @NotBlank @Email String email,
    @NotBlank String name,
    @NotBlank String alertType,
    @NotBlank String reason
) {}