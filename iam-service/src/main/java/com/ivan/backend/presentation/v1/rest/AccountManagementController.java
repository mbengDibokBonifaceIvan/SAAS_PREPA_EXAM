package com.ivan.backend.presentation.v1.rest;

import com.ivan.backend.application.dto.ProvisionUserRequest;
import com.ivan.backend.application.dto.UpdateUserRequest;
import com.ivan.backend.application.dto.UserResponse;
import com.ivan.backend.application.port.in.ManageAccountInputPort;
import com.ivan.backend.application.port.in.ProvisionUserInputPort;
import com.ivan.backend.application.port.in.SearchUserInputPort;
import com.ivan.backend.application.port.in.UpdateUserInputPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Gestion des Comptes", description = "Endpoints pour l'administration et la consultation des utilisateurs")
@SecurityRequirement(name = "bearerAuth") // Indique que l'API nécessite un JWT
public class AccountManagementController {

    private final ProvisionUserInputPort provisionUserUseCase;
    private final ManageAccountInputPort manageAccountInputPort;
    private final SearchUserInputPort searchUserPort;
    private final UpdateUserInputPort updateUserInputPort;

    private static final String EMAIL = "email";

    /**
     * Endpoint pour provisionner un nouveau compte (Staff ou Candidat).
     * Accessible uniquement par CENTER_OWNER, UNIT_MANAGER ou STAFF_MEMBER.
     */
    @Operation(summary = "Provisionner un compte", description = "Crée un nouvel utilisateur (Staff ou Candidat) et déclenche l'envoi d'un email d'activation.")

    @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @ApiResponse(responseCode = "403", description = "Droits insuffisants (Nécessite OWNER, MANAGER ou STAFF)")
    @PostMapping("/provision")
    @PreAuthorize("hasAnyRole('CENTER_OWNER', 'UNIT_MANAGER', 'STAFF_MEMBER')")
    public ResponseEntity<Map<String, String>> provisionAccount(
            @Valid @RequestBody ProvisionUserRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt // On récupère le JWT de celui qui appelle l'API
    ) {
        // L'email du créateur est généralement dans le claim 'sub' ou 'email' du JWT
        String creatorEmail = jwt.getClaimAsString(EMAIL);

        provisionUserUseCase.execute(request, creatorEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Utilisateur provisionné avec succès. Un email d'activation lui a été envoyé."));
    }

    @Operation(summary = "Bannir un utilisateur", description = "Désactive l'accès d'un utilisateur au système.")
    @PatchMapping("/{email}/ban")
    @PreAuthorize("hasRole('CENTER_OWNER')")
    public ResponseEntity<Void> ban(
            @Parameter(description = "Email de l'utilisateur à bannir") @PathVariable String email,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt // <--- Utilise JWT ici
    ) {
        String ownerEmail = jwt.getClaimAsString(EMAIL); // On récupère le vrai email
        manageAccountInputPort.banAccount(email, ownerEmail);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Réactiver un compte", description = "Rétablit l'accès d'un utilisateur précédemment banni.")
    @PatchMapping("/{email}/activate")
    @PreAuthorize("hasRole('CENTER_OWNER')")
    public ResponseEntity<Void> activate(
            @PathVariable String email,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt // <--- Et ici aussi
    ) {
        String ownerEmail = jwt.getClaimAsString(EMAIL);
        manageAccountInputPort.activateAccount(email, ownerEmail);
        return ResponseEntity.noContent().build();
    }

    /**
     * UC7: Récupérer mon propre profil
     */
    @Operation(summary = "Récupérer mon profil", description = "Retourne les informations de l'utilisateur actuellement connecté.")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString(EMAIL);
        UserResponse user = UserResponse.fromDomain(searchUserPort.getUserProfile(email));
        return ResponseEntity.ok(user);
    }

    /**
     * UC8 & UC9: Récupérer l'annuaire selon mes droits
     */
    @Operation(summary = "Consulter l'annuaire", description = "Récupère la liste des utilisateurs. Les managers ne voient que les membres de leur unité.")
    @GetMapping("/directory")
    @PreAuthorize("hasAnyRole('CENTER_OWNER', 'UNIT_MANAGER')")
    public ResponseEntity<List<UserResponse>> getDirectory(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID de l'unité pour filtrer (optionnel)") @RequestParam(required = false) UUID unitId // Paramètre
                                                                                                                           // optionnel
                                                                                                                           // :
                                                                                                                           // ?unitId=...
    ) {
        String email = jwt.getClaimAsString(EMAIL);
        List<UserResponse> users = searchUserPort.getDirectory(email, unitId).stream()
                .map(UserResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Récupérer un utilisateur par son ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CENTER_OWNER', 'UNIT_MANAGER', 'STAFF_MEMBER')")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        // Il faudra ajouter cette méthode dans ton UseCase
        UserResponse user = UserResponse.fromDomain(searchUserPort.getUserById(id, jwt.getClaimAsString(EMAIL)));
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Mettre à jour un utilisateur")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CENTER_OWNER', 'UNIT_MANAGER', 'STAFF_MEMBER')")
    public ResponseEntity<Void> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        updateUserInputPort.execute(id, jwt.getClaimAsString(EMAIL), request);
        return ResponseEntity.noContent().build();
    }
}