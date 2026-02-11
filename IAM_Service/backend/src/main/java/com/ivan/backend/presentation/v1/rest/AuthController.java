package com.ivan.backend.presentation.v1.rest;

import com.ivan.backend.application.dto.LoginRequest;
import com.ivan.backend.application.dto.LoginResponse;
import com.ivan.backend.application.port.in.LoginInputPort;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Gestion des sessions utilisateurs et jetons de sécurité")
public class AuthController {

    // On injecte l'interface (Port d'entrée) et non l'implémentation
    private final LoginInputPort loginInputPort;

    @Operation(summary = "Connexion utilisateur", description = "Authentifie un utilisateur avec son email et mot de passe. Retourne un couple Access Token / Refresh Token.")

    @ApiResponse(responseCode = "200", description = "Authentification réussie", content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(responseCode = "401", description = "Identifiants invalides (Email ou mot de passe incorrect)", content = @Content // Pas
                                                                                                                                    // de
                                                                                                                                    // corps
                                                                                                                                    // spécifique
                                                                                                                                    // ou
                                                                                                                                    // utilise
                                                                                                                                    // ton
                                                                                                                                    // ProblemDetail
    )
    @ApiResponse(responseCode = "403", description = "Compte banni ou non activé", content = @Content)
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // On communique via des DTO uniquement
        LoginResponse response = loginInputPort.login(request);
        return ResponseEntity.ok(response);
    }
}