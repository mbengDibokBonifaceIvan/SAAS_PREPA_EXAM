package com.ivan.backend.presentation.v1.rest;

import com.ivan.backend.application.dto.LogoutRequest;
import com.ivan.backend.application.port.in.LogoutInputPort;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // N'oublie pas l'annotation RestController !
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class LogoutController {

    private final LogoutInputPort logoutInputPort; 

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        logoutInputPort.execute(request.refreshToken()); // Appel sur l'instance injectée
        return ResponseEntity.noContent().build();
    }
}