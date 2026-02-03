package com.ivan.backend.domain.entity;

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

}
