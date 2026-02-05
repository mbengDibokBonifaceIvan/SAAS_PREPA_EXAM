package com.ivan.backend.application.dto;

import com.ivan.backend.domain.valueobject.UserRole;
import java.util.UUID;

public record ProvisionUserRequest(
    String firstName,
    String lastName,
    String email,
    UserRole role,
    UUID unitId // Peut être null si CENTER_OWNER crée un autre admin global
) {}