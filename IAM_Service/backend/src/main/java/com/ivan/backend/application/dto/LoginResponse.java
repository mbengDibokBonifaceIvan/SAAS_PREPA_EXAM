package com.ivan.backend.application.dto;

public record LoginResponse (
    String accessToken,
    String refreshToken,
    long expiresIn,
    String email,
    boolean mustChangePassword,
    String role
){
    
}
