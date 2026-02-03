package com.ivan.backend.domain.valueobject;

public record ProviderStatus(
    boolean isEmailVerified, 
    boolean mustChangePassword
) {}