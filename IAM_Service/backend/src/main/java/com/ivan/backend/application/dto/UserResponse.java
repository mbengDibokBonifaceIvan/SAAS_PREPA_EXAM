package com.ivan.backend.application.dto;

import java.util.UUID;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.UserRole;

public record UserResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    UserRole role,
    UUID tenantId,
    UUID unitId,
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
