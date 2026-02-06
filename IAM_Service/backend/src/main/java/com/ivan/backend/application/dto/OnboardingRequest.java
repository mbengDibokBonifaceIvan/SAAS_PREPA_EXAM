package com.ivan.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OnboardingRequest(
    @NotBlank(message = "Le prénom est obligatoire")
    String firstName,
    @NotBlank(message = "Le nom de famille est obligatoire")
    String lastName,
    @NotBlank(message = "L'email est obligatoire") @Email(message = "L'email n'est pas valide")
    String email,
    @NotBlank (message = "Le mot de passe est obligatoire")
    String password,
    @NotBlank (message = "Le nom du centre est obligatoire")
    String organizationName
) {}