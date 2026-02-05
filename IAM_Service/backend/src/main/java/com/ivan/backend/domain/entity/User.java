package com.ivan.backend.domain.entity;

import com.ivan.backend.domain.exception.DomainException;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import lombok.Getter;

import java.util.UUID;

@Getter
public class User {
    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final Email email;
    private final UUID tenantId; // ID du centre
    private final UUID unitId; // ID du sous centre (peut être null)
    private final UserRole role;

    // Champs de statut (Non-final car ils évoluent)
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
     * Synchronise le statut d'activation basé sur la validation Keycloak.
     * Si l'email est validé, on active automatiquement le compte.
     */
    public void syncValidationStatus(boolean isEmailVerifiedInProvider) {
        if (isEmailVerifiedInProvider) {
            this.emailVerified = true;
            this.isActive = true;
        }
    }

    /**
     * Définit si l'utilisateur doit changer son mot de passe au prochain login.
     */
    public void updatePasswordRequirement(boolean required) {
        this.mustChangePassword = required;
    }

    /**
     * Désactive le compte utilisateur.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Active le compte utilisateur.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Vérifie si cet utilisateur (le créateur) a le droit d'en créer un autre.
     */
    public void validateCanCreate(UserRole targetRole, UUID targetTenantId, UUID targetUnitId) {
        // 1. Règle de cloisonnement (Tenant/Centre)
        if (!this.tenantId.equals(targetTenantId)) {
            throw new DomainException("Interdit : Impossible de créer un utilisateur pour un autre centre.");
        }

        // 2. Règle de Hiérarchie
        if (!this.role.isHigherThan(targetRole)) {
            throw new DomainException("Interdit : Vous ne pouvez pas créer un rôle égal ou supérieur au vôtre.");
        }

        // 3. Règle de Scope (Unité/Sous-centre)
        // Le CENTER_OWNER peut créer partout dans son centre (unitId peut être null ou
        // non)
        // Mais le UNIT_MANAGER et STAFF ne peuvent créer que dans leur PROPRE unité.
        if (this.role != UserRole.CENTER_OWNER && (this.unitId == null || !this.unitId.equals(targetUnitId))) {
            throw new DomainException("Interdit : Vous ne pouvez créer des membres que pour votre propre unité.");
        }
    }
}
