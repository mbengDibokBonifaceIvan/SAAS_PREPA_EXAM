package com.ivan.notification_service.domain.exception;

public class NotificationDomainException extends RuntimeException {
    public NotificationDomainException(String message) {
        super(message);
    }
}