package com.ivan.backend.presentation.v1.rest;

import com.ivan.backend.application.dto.LogoutRequest;
import com.ivan.backend.application.port.in.LogoutInputPort;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification") // Regroupe cet endpoint avec le login
public class LogoutController {

    private final LogoutInputPort logoutInputPort;

    @Operation(summary = "Déconnexion utilisateur", description = "Invalide le Refresh Token fourni pour mettre fin à la session de l'utilisateur.", security = @SecurityRequirement(name = "bearerAuth") // Indique
                                                                                                                                                                                                          // que
                                                                                                                                                                                                          // le
                                                                                                                                                                                                          // JWT
                                                                                                                                                                                                          // est
                                                                                                                                                                                                          // requis
    )

    @ApiResponse(responseCode = "204", description = "Déconnexion réussie, le jeton a été invalidé")
    @ApiResponse(responseCode = "401", description = "Jeton d'accès manquant ou invalide")
    @ApiResponse(responseCode = "400", description = "Refresh Token manquant dans le corps de la requête")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        logoutInputPort.execute(request.refreshToken()); // Appel sur l'instance injectée
        return ResponseEntity.noContent().build();
    }
}