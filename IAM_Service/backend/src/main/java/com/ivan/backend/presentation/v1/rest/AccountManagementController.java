package com.ivan.backend.presentation.v1.rest;

import com.ivan.backend.application.dto.ProvisionUserRequest;
import com.ivan.backend.application.port.ProvisionUserInputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class AccountManagementController {

    private final ProvisionUserInputPort provisionUserUseCase;

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
        String creatorEmail = jwt.getClaimAsString("email");

        provisionUserUseCase.execute(request, creatorEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Utilisateur provisionné avec succès. Un email d'activation lui a été envoyé."
        ));
    }
}