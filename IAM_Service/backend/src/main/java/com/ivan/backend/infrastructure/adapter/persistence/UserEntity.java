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
    
    private boolean isActive = false;
    private boolean mustChangePassword = true;

    // PasswordReset, RefreshToken et AuditLog seront des entités liées (OneToMany) plus tard
}