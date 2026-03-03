package com.ivan.backend.domain.exception;

public class ResourceAccessDeniedException extends DomainException {
    public ResourceAccessDeniedException(String message) { super(message); }
}