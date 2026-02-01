package com.ivan.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OnboardingRequest(
    @NotBlank
    String firstName,
    @NotBlank
    String lastName,
    @NotBlank @Email
    String email,
    @NotBlank
    String password,
    @NotBlank
    String organizationName
) {}