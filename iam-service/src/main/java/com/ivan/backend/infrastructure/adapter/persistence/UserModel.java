package com.ivan.backend.infrastructure.adapter.persistence;

import com.ivan.backend.domain.valueobject.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
public class UserModel extends AuditableModel { // Héritage de l'audit

    @Id
    private UUID userId;

    private String firstName;
    private String lastName;
    
    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private UUID externalOrganizationId;
    private UUID externalUnitId = null;
    
    private boolean isActive = false;
    private boolean mustChangePassword = false;
    private boolean isEmailVerified = false;

    // PLUS BESOIN de createdAt/updatedAt ici, ils sont dans AuditableModel
}