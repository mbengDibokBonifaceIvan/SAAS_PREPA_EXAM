package com.ivan.backend.application.dto;

import java.util.UUID;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Représentation complète des informations d'un utilisateur pour les réponses API")
public record UserResponse(
    
    @Schema(description = "Identifiant unique de l'utilisateur", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    
    @Schema(description = "Prénom de l'utilisateur", example = "Ivan")
    String firstName,
    
    @Schema(description = "Nom de famille de l'utilisateur", example = "Zaitsev")
    String lastName,
    
    @Schema(description = "Adresse email professionnelle", example = "ivan.z@skilyo.com")
    String email,
    
    @Schema(description = "Rôle métier au sein de la plateforme")
    UserRole role,
    
    @Schema(description = "ID de l'organisation (Tenant) propriétaire du compte", example = "a1b2c3d4-e5f6-7890-abcd-1234567890ab")
    UUID tenantId,
    
    @Schema(description = "ID de l'unité de rattachement (peut être null si non assigné)", example = "b9f8e7d6-c5b4-a321-0987-654321098765")
    UUID unitId,
    
    @Schema(description = "Indique si le compte est actif (true) ou banni/suspendu (false)", example = "true")
    boolean active
) {
    public static UserResponse fromDomain(User user) {
        return new UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail().value(),
            user.getRole(),
            user.getTenantId(),
            user.getUnitId(),
            user.isActive()
        );
    }
}
