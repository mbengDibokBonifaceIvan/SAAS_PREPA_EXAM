package com.ivan.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
    @NotBlank(message = "L'email est obligatoire") @Email(message = "L'email n'est pas valide")
    String email
) {}

