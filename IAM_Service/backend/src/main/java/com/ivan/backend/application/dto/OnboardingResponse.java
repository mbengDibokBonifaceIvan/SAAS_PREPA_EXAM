package com.ivan.backend.application.dto;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Détails du compte créé après onboarding")
public record OnboardingResponse(
    String firstName,
    String lastName,
    boolean isActive,
    @Schema(description = "ID unique de l'organisation créée") UUID externalOrganizationId,
    boolean mustChangePassword,
    boolean isEmailVerified
) {}