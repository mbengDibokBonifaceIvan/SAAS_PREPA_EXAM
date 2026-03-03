package com.ivan.backend.application.dto;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ivan.backend.domain.valueobject.UserRole;

@Schema(description = "Requête de mise à jour partielle d'un utilisateur")
public record UpdateUserRequest(
    @Schema(example = "NouveauPrénom") String firstName,
    @Schema(example = "NouveauNom") String lastName,
    @Schema(description = "Modification du rôle (Owner uniquement)") UserRole role,
    @Schema(description = "Changement d'unité") UUID unitId
) {}