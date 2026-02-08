package com.ivan.backend.presentation.v1.rest;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import com.ivan.backend.application.dto.ForgotPasswordRequest;
import com.ivan.backend.application.port.in.PasswordResetInputPort;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordResetInputPort passwordResetInputPort;

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetInputPort.requestReset(request.email());
        
        return ResponseEntity.ok(Map.of(
            "message", "Si un compte est associé à cet email, une procédure de réinitialisation a été envoyée."
        ));
    }
}

