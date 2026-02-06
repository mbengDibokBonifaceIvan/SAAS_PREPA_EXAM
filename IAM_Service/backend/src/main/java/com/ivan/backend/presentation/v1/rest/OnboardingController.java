package com.ivan.backend.presentation.v1.rest;

import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.dto.OnboardingResponse;
import com.ivan.backend.application.port.in.OnboardingInputPort;

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
public class OnboardingController {

    private final OnboardingInputPort onboardingInputPort;

    @PostMapping("/onboarding")
    public ResponseEntity<OnboardingResponse> registerOrganization(
            @Valid @RequestBody OnboardingRequest request) {
        
        // Appel de la couche application
        OnboardingResponse response = onboardingInputPort.execute(request);
        
        // Retourne 201 Created selon ton contrat OpenAPI
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}