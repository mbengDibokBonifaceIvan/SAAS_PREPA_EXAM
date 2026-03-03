package com.ivan.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Requête de déconnexion")
public record LogoutRequest(
    @Schema(description = "Le refresh token à invalider")
    @NotBlank(message = "Le refresh token est obligatoire")
    String refreshToken
) {}
