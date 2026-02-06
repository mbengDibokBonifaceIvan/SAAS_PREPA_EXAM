package com.ivan.backend.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
    @NotBlank(message = "Le refresh token est obligatoire")
    String refreshToken
) {}
