package com.ivan.backend.infrastructure.adapter.persistence;

import com.ivan.backend.domain.valueobject.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
public class UserEntity {

    @Id
    private UUID userId;

    private String firstName;
    private String lastName;
    
    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private UserRole role; // On simplifie l'enum pour l'instant

    private UUID externalOrganizationId;
    private UUID externalUnitId = null;
    
    private boolean isActive = false;
    private boolean mustChangePassword = false;
    private boolean isEmailVerified = false;
    
    // Audit simple (Toujours utile en base de données)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    private java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now();

    // PasswordReset, RefreshToken et AuditLog seront des entités liées (OneToMany) plus tard
}