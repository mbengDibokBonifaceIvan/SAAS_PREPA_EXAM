package com.ivan.backend.presentation.v1.rest;

import com.ivan.backend.application.dto.LoginRequest;
import com.ivan.backend.application.dto.LoginResponse;
import com.ivan.backend.application.port.LoginInputPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    // On injecte l'interface (Port d'entrée) et non l'implémentation
    private final LoginInputPort loginUseCase;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // On communique via des DTO uniquement
        LoginResponse response = loginUseCase.login(request);
        return ResponseEntity.ok(response);
    }
}