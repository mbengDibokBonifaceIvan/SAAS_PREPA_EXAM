package com.ivan.backend.application.dto;

import com.ivan.backend.domain.valueobject.UserRole;

import jakarta.validation.constraints.*;

import java.util.UUID;


public record ProvisionUserRequest(
    @NotBlank(message = "Le prénom est obligatoire")
    String firstName,
    
    @NotBlank(message = "Le nom est obligatoire")
    String lastName,
    
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    String email,
    
    @NotNull(message = "Le rôle est obligatoire")
    UserRole role,
    
    UUID unitId 
) {}