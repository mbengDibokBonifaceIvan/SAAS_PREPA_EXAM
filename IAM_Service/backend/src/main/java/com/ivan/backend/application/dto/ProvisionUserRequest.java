package com.ivan.backend.application.dto;

import com.ivan.backend.domain.valueobject.UserRole;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;


@Schema(description = "Requête de provisionnement par un administrateur")
public record ProvisionUserRequest(
    @Schema(example = "Marc") @NotBlank String firstName,
    @Schema(example = "Lavoine") @NotBlank String lastName,
    @Schema(example = "m.lavoine@unit.com") @NotBlank @Email String email,
    @Schema(description = "CANDIDATE") @NotNull UserRole role,
    @Schema(description = "ID de l'unité de rattachement (optionnel)") UUID unitId 
) {}