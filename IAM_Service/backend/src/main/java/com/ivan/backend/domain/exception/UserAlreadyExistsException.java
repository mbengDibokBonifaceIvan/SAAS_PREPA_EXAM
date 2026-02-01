package com.ivan.backend.domain.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email) {
        super("L'utilisateur avec l'email " + email + " existe déjà.");
    }
}