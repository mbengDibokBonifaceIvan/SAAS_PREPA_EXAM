package com.ivan.backend.application.dto;

import java.util.UUID;

public record OnboardingResponse(
    String firstName,
    String lastName,
    boolean isActive,
    UUID externalOrganizationId,
    boolean mustChangePassword,
    boolean isEmailVerified
) {}