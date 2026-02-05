package com.ivan.backend.application.dto;

import java.util.UUID;

import com.ivan.backend.domain.valueobject.UserRole;

public record UpdateUserRequest(
    String firstName,
    String lastName,
    UserRole role, // Optionnel : seul l'Owner peut changer ça
    UUID unitId    // Optionnel : seul l'Owner peut changer ça
) {}