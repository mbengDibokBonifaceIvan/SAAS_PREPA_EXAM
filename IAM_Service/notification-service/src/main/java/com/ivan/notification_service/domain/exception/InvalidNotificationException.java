package com.ivan.notification_service.domain.exception;

import lombok.Getter;

@Getter
public class InvalidNotificationException extends NotificationDomainException {
    
    private final String fieldName;

    public InvalidNotificationException(String message) {
        super(message);
        this.fieldName = null;
    }

    public InvalidNotificationException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }
}