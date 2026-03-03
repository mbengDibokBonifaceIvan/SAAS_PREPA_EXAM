package com.ivan.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requête pour la récupération de mot de passe")
public record ForgotPasswordRequest(
    @Schema(description = "Email de l'utilisateur", example = "amougou.jean@gmail.com")
    @NotBlank(message = "L'email est obligatoire") @Email(message = "L'email n'est pas valide")
    String email
) {}

