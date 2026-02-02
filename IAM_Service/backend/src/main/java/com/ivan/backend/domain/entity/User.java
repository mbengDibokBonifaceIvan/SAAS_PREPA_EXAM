package com.ivan.backend.domain.entity;

import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter 
@Setter
public class User {
    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final Email email;
    private final UUID tenantId; // ID du centre
    private final UUID unitId; // ID du sous centre (peut être null)
    private final UserRole role;
    private boolean emailVerified = false;

    public User(UUID id, String firstName, String lastName, Email email, UUID tenantId, UUID unitId, UserRole role, boolean emailVerified) {
        this.id = (id == null) ? UUID.randomUUID() : id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.tenantId = tenantId;
        this.unitId = unitId;
        this.role = role;
        this.emailVerified = emailVerified;
    }
}
