package com.ivan.backend.presentation.v1.rest;

import com.ivan.backend.application.dto.ProvisionUserRequest;
import com.ivan.backend.application.dto.UpdateUserRequest;
import com.ivan.backend.application.dto.UserResponse;
import com.ivan.backend.application.port.in.ManageAccountInputPort;
import com.ivan.backend.application.port.in.ProvisionUserInputPort;
import com.ivan.backend.application.port.in.SearchUserInputPort;
import com.ivan.backend.application.port.in.UpdateUserInputPort;

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
    @PostMapping("/provision")
    @PreAuthorize("hasAnyRole('CENTER_OWNER', 'UNIT_MANAGER', 'STAFF_MEMBER')")
    public ResponseEntity<Map<String, String>> provisionAccount(
            @RequestBody ProvisionUserRequest request,
            @AuthenticationPrincipal Jwt jwt // On récupère le JWT de celui qui appelle l'API
    ) {
        // L'email du créateur est généralement dans le claim 'sub' ou 'email' du JWT
        String creatorEmail = jwt.getClaimAsString(EMAIL);

        provisionUserUseCase.execute(request, creatorEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Utilisateur provisionné avec succès. Un email d'activation lui a été envoyé."));
    }

    @PatchMapping("/{email}/ban")
    @PreAuthorize("hasRole('CENTER_OWNER')")
    public ResponseEntity<Void> ban(
            @PathVariable String email,
            @AuthenticationPrincipal Jwt jwt // <--- Utilise JWT ici
    ) {
        String ownerEmail = jwt.getClaimAsString(EMAIL); // On récupère le vrai email
        manageAccountInputPort.banAccount(email, ownerEmail);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{email}/activate")
    @PreAuthorize("hasRole('CENTER_OWNER')")
    public ResponseEntity<Void> activate(
            @PathVariable String email,
            @AuthenticationPrincipal Jwt jwt // <--- Et ici aussi
    ) {
        String ownerEmail = jwt.getClaimAsString(EMAIL);
        manageAccountInputPort.activateAccount(email, ownerEmail);
        return ResponseEntity.noContent().build();
    }

    /**
     * UC7: Récupérer mon propre profil
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString(EMAIL);
        UserResponse user = UserResponse.fromDomain(searchUserPort.getUserProfile(email));
        return ResponseEntity.ok(user);
    }

    /**
     * UC8 & UC9: Récupérer l'annuaire selon mes droits
     */
    @GetMapping("/directory")
    @PreAuthorize("hasAnyRole('CENTER_OWNER', 'UNIT_MANAGER')")
    public ResponseEntity<List<UserResponse>> getDirectory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID unitId // Paramètre optionnel : ?unitId=...
    ) {
        String email = jwt.getClaimAsString(EMAIL);
        List<UserResponse> users = searchUserPort.getDirectory(email, unitId).stream()
                .map(UserResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CENTER_OWNER', 'UNIT_MANAGER')")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        // Il faudra ajouter cette méthode dans ton UseCase
        UserResponse user = UserResponse.fromDomain(searchUserPort.getUserById(id, jwt.getClaimAsString(EMAIL)));
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CENTER_OWNER', 'UNIT_MANAGER', 'STAFF_MEMBER')")
    public ResponseEntity<Void> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        updateUserInputPort.execute(id, jwt.getClaimAsString(EMAIL), request);
        return ResponseEntity.noContent().build();
    }
}