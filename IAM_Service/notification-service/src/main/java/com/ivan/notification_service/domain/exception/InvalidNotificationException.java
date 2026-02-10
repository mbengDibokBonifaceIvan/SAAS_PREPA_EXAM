package com.ivan.notification_service.domain.exception;

public class InvalidNotificationException extends NotificationDomainException {
    public InvalidNotificationException(String message) {
        super(message);
    }
}