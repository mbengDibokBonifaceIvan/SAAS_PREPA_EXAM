package com.ivan.backend.presentation.v1.rest;

import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.dto.OnboardingResponse;
import com.ivan.backend.application.port.in.OnboardingInputPort;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Gestion des inscriptions et des sessions")
public class OnboardingController {

    private final OnboardingInputPort onboardingInputPort;

    @Operation(summary = "Créer une nouvelle organisation (Onboarding)", description = "Inscrit un nouvel utilisateur administrateur et crée son organisation (Tenant). "
            +
            "Cette route est publique et ne nécessite pas de jeton d'authentification.")
    @ApiResponse(responseCode = "201", description = "Organisation et compte administrateur créés avec succès", content = @Content(schema = @Schema(implementation = OnboardingResponse.class)))
    @ApiResponse(responseCode = "400", description = "Données d'inscription invalides (ex: email déjà utilisé ou format incorrect)", content = @Content)
    @ApiResponse(responseCode = "409", description = "Conflit : L'organisation ou l'email existe déjà", content = @Content)
    @PostMapping("/onboarding")
    public ResponseEntity<OnboardingResponse> registerOrganization(
            @Valid @RequestBody OnboardingRequest request) {

        // Appel de la couche application
        OnboardingResponse response = onboardingInputPort.execute(request);

        // Retourne 201 Created selon ton contrat OpenAPI
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}