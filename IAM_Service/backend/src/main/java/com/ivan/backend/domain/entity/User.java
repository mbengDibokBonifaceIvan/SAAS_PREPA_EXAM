package com.ivan.backend.domain.entity;

import com.ivan.backend.domain.exception.*;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import lombok.Getter;

import java.util.UUID;

@Getter
public class User {
    private final UUID id;
    private String firstName;
    private String lastName;
    private final Email email;
    private final UUID tenantId; 
    private UUID unitId; 
    private UserRole role;

    private boolean emailVerified;
    private boolean isActive;
    private boolean mustChangePassword;

    public User(UUID id, String firstName, String lastName, Email email,
            UUID tenantId, UUID unitId, UserRole role,
            boolean emailVerified, boolean isActive,
            boolean mustChangePassword) {
        this.id = (id == null) ? UUID.randomUUID() : id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.tenantId = tenantId;
        this.unitId = unitId;
        this.role = role;
        this.emailVerified = emailVerified;
        this.isActive = isActive;
        this.mustChangePassword = mustChangePassword;
    }

    // --- LOGIQUE MÉTIER ---

    /**
     * Centralise la vérification des droits de gestion d'un utilisateur sur un autre.
     * Utilisé pour le bannissement, l'activation, et la modification de profil.
     */
    public void checkCanManage(User target) {
        // 1. Isolation Multi-tenant (Périmètre du centre)
        if (!this.tenantId.equals(target.getTenantId())) {
            throw new ResourceAccessDeniedException("Accès refusé : l'utilisateur appartient à un autre centre.");
        }

        // Autorisation si c'est soi-même
        if (this.id.equals(target.getId())) return;

        // 2. Règle de Hiérarchie
        if (!this.role.isHigherThan(target.getRole())) {
            throw new InsufficientPrivilegesException("Interdit : vous ne pouvez pas gérer un rang égal ou supérieur au vôtre.");
        }

        // 3. Règle de Scope (Unité/Sous-centre)
        if (this.role != UserRole.CENTER_OWNER && (this.unitId == null || !this.unitId.equals(target.getUnitId()))) {
            throw new ResourceAccessDeniedException("Interdit : cet utilisateur n'appartient pas à votre unité.");
        }
    }

    public void validateCanCreate(UserRole targetRole, UUID targetTenantId, UUID targetUnitId) {
        if (!this.tenantId.equals(targetTenantId)) {
            throw new ResourceAccessDeniedException("Interdit : Impossible de créer un utilisateur pour un autre centre.");
        }

        if (!this.role.isHigherThan(targetRole)) {
            throw new InsufficientPrivilegesException("Interdit : Vous ne pouvez pas créer un rôle égal ou supérieur au vôtre.");
        }

        if (this.role != UserRole.CENTER_OWNER && (this.unitId == null || !this.unitId.equals(targetUnitId))) {
            throw new ResourceAccessDeniedException("Interdit : Vous ne pouvez créer des membres que pour votre propre unité.");
        }
    }

    public void updateProfile(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            throw new BusinessRuleViolationException("Le prénom ne peut pas être vide.");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new BusinessRuleViolationException("Le nom ne peut pas être vide.");
        }
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void changeRole(UserRole newRole, UUID requesterId) {
        if (this.id.equals(requesterId)) {
            throw new InsufficientPrivilegesException("Sécurité : Vous ne pouvez pas modifier votre propre rôle.");
        }
        this.role = newRole;
    }

    public void syncValidationStatus(boolean isEmailVerifiedInProvider) {
        if (isEmailVerifiedInProvider) {
            this.emailVerified = true;
            this.isActive = true;
        }
    }

    public void updatePasswordRequirement(boolean required) {
        this.mustChangePassword = required;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void assignToUnit(UUID newUnitId) {
        this.unitId = newUnitId;
    }
}