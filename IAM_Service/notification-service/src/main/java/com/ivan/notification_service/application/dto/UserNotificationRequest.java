package com.ivan.notification_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import jakarta.validation.constraints.Email;

// DTO pour les notifications utilisateur simples (Reset, Activation, Provisioning, Welcome)
public record UserNotificationRequest(
    @NotNull UUID userId,
    @NotBlank @Email String email,
    @NotBlank String name,
    String detail // Utilisé pour 'orgName', 'role', ou 'reason' selon le contexte
) {}