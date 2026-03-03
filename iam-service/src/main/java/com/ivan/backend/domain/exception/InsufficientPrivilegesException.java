package com.ivan.backend.domain.exception;

// 403 Forbidden - Violation de hiérarchie ou de périmètre
public class InsufficientPrivilegesException extends DomainException {
    public InsufficientPrivilegesException(String message) {
        super(message);
    }
}