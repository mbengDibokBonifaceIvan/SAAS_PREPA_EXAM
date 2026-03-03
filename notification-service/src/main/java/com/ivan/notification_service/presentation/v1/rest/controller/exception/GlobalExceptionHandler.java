package com.ivan.notification_service.presentation.v1.rest.controller.exception;

import com.ivan.notification_service.domain.exception.InvalidNotificationException;
import com.ivan.notification_service.domain.exception.NotificationDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP = "timestamp";

    private ProblemDetail buildProblem(HttpStatus status, String title, String detail, String errorCode) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://api.exams.com/errors/" + errorCode));
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }

    // --- DOMAIN EXCEPTIONS ---

    @ExceptionHandler(InvalidNotificationException.class)
    public ProblemDetail handleInvalidNotification(InvalidNotificationException ex) {
        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Notification Invalide",
                ex.getMessage(),
                "invalid-notification-data");

        if (ex.getFieldName() != null) {
            problem.setProperty("invalid_field", ex.getFieldName());
        }

        return problem;
    }

    @ExceptionHandler(NotificationDomainException.class)
    public ProblemDetail handleDomainException(NotificationDomainException ex) {
        return buildProblem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Erreur Métier",
                ex.getMessage(),
                "domain-business-error");
    }

    // --- INFRASTRUCTURE & GENERAL ---

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        return buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erreur Système",
                "Une erreur inattendue est survenue dans le service de notification.",
                "internal-server-error");
    }

    // --- PRESENTATION LAYER ---

    // Gestion des paramètres @RequestParam manquants
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParams(MissingServletRequestParameterException ex) {
        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Paramètre manquant",
                String.format("Le paramètre '%s' est obligatoire pour cet endpoint.", ex.getParameterName()),
                "missing-request-parameter");
    }

    // Gestion des erreurs de validation @Valid sur les @RequestBody
    // (FeedbackRequest)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce("", (a, b) -> a + b + "; ");

        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Erreur de validation",
                detail,
                "validation-failed");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // On sécurise la récupération du nom du type
        String requiredType = Optional.ofNullable(ex.getRequiredType())
                .map(Class::getSimpleName)
                .orElse("inconnu");

        String detail = String.format("La valeur '%s' est invalide pour le paramètre '%s'. Un type %s est attendu.",
                ex.getValue(), ex.getName(), requiredType);

        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Type de paramètre invalide",
                detail,
                "invalid-parameter-type");
    }
}