package com.ivan.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse suite à une connexion réussie")
public record LoginResponse (
    @Schema(description = "JWT Access Token") String accessToken,
    @Schema(description = "Refresh Token pour renouveler la session") String refreshToken,
    @Schema(description = "Durée de validité en secondes", example = "3600") long expiresIn,
    @Schema(description = "Email de l'utilisateur", example = "admin@skilyo.com") String email,
    @Schema(description = "Indique si l'utilisateur doit changer son mot de passe au prochain login") boolean mustChangePassword,
    @Schema(description = "Rôle de l'utilisateur", example = "CENTER_OWNER") String role
) {}
