package com.ivan.notification_service.presentation.v1.rest.controller.exception;

import com.ivan.notification_service.domain.exception.NotificationDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class PresentationExceptionHandler {

    @ExceptionHandler(NotificationDomainException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleDomainError(NotificationDomainException ex) {
        return Map.of("error", ex.getMessage());
    }
}