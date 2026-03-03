package com.ivan.backend.domain.valueobject;

public record AuthToken(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    String tokenType
) {}
