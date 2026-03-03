package com.ivan.backend.presentation.v1.rest;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import com.ivan.backend.application.dto.ForgotPasswordRequest;
import com.ivan.backend.application.port.in.PasswordResetInputPort;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Gestion des sessions et récupération de compte")
public class PasswordController {

    private final PasswordResetInputPort passwordResetInputPort;

    @Operation(summary = "Demande de réinitialisation de mot de passe", description = "Initie la procédure de récupération. Si l'email correspond à un compte actif, un lien de réinitialisation est envoyé par email.")
    @ApiResponse(responseCode = "200", description = "Requête traitée (la réponse est identique que l'email existe ou non pour des raisons de sécurité)", content = @Content(examples = @ExampleObject(value = "{\"message\": \"Si un compte est associé à cet email, une procédure de réinitialisation a été envoyée.\"}")))
    @ApiResponse(responseCode = "400", description = "Format d'email invalide")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetInputPort.requestReset(request.email());

        return ResponseEntity.ok(Map.of(
                "message", "Si un compte est associé à cet email, une procédure de réinitialisation a été envoyée."));
    }
}
