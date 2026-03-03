package com.ivan.backend.domain.exception;

// 409 Conflict - Tentative de création d'un doublon
public class UserAlreadyExistsException extends DomainException {
    public UserAlreadyExistsException(String email) {
        super("L'utilisateur avec l'email " + email + " existe déjà.");
    }
}