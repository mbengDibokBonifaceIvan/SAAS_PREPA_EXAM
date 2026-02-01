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
    private final UUID tenantId; // ID de l'organisation
    private final UserRole role;

    public User(UUID id, String firstName, String lastName, Email email, UUID tenantId, UserRole role) {
        this.id = (id == null) ? UUID.randomUUID() : id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.tenantId = tenantId;
        this.role = role;
    }
}