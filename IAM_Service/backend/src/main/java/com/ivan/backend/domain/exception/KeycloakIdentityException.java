package com.ivan.backend.domain.exception;

public class KeycloakIdentityException extends RuntimeException {
    public KeycloakIdentityException(String message) {
        super(message);
    }

    public KeycloakIdentityException(String message, Throwable cause) {
        super(message, cause);
    }
}
