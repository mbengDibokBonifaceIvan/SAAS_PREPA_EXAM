package com.ivan.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Requête de création d'organisation (Self-Onboarding)")
public record OnboardingRequest(
    @Schema(example = "Ivan") @NotBlank(message = "Le prénom est obligatoire") String firstName,
    @Schema(example = "Dalton") @NotBlank(message = "Le nom de famille est obligatoire") String lastName,
    @Schema(example = "contact@skilyo.com") @NotBlank(message = "L'email est obligatoire") @Email String email,
    @Schema(description = "Mot de passe choisi", example = "StrongPwd123!") @NotBlank String password,
    @Schema(description = "Nom du centre", example = "Skilyo Org") @NotBlank String organizationName
) {}