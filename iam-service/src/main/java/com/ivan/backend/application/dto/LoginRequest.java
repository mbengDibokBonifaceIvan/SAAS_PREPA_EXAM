package com.ivan.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Identifiants de connexion")
public record LoginRequest(
    @Schema(description = "Email de l'utilisateur", example = "admin@skilyo.com")
    @NotBlank(message = "L'email est obligatoire") @Email(message = "Format d'email invalide")
    String email, 
    
    @Schema(description = "Mot de passe", example = "Secret123!")
    @NotBlank(message = "Le mot de passe est obligatoire")
    String password
) {}