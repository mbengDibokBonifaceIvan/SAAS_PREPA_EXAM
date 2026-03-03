package com.ivan.backend.domain.exception;

// 403 Forbidden - Sécurité métier (Tentatives échouées)
public class AccountLockedException extends DomainException {
    public AccountLockedException(String message) {
        super(message);
    }
}
