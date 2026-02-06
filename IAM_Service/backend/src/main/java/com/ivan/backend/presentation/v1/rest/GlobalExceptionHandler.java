package com.ivan.backend.presentation.v1.rest;

import com.ivan.backend.domain.exception.AccountLockedException;
import com.ivan.backend.domain.exception.BusinessRuleViolationException;
import com.ivan.backend.domain.exception.DomainException;
import com.ivan.backend.domain.exception.InsufficientPrivilegesException;
import com.ivan.backend.domain.exception.ResourceAccessDeniedException;
import com.ivan.backend.domain.exception.UserAlreadyExistsException;
import com.ivan.backend.infrastructure.adapter.identity.exception.KeycloakIdentityException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP = "timestamp";

    /**
     * Helper pour centraliser la création des ProblemDetail
     */
    private ProblemDetail buildProblem(HttpStatus status, String title, String detail, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        if (path != null) {
            problem.setType(URI.create("https://api.exams.com/errors/" + path));
        }
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }

    // --- DOMAIN & BUSINESS EXCEPTIONS ---

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserExists(UserAlreadyExistsException ex) {
        return buildProblem(HttpStatus.CONFLICT, "Conflit d'identité", ex.getMessage(), "user-already-exists");
    }

    @ExceptionHandler(AccountLockedException.class)
    public ProblemDetail handleAccountLocked(AccountLockedException ex) {
        return buildProblem(HttpStatus.FORBIDDEN, "Compte Verrouillé", ex.getMessage(), "account-locked");
    }

    @ExceptionHandler(InsufficientPrivilegesException.class)
    public ProblemDetail handlePrivileges(InsufficientPrivilegesException ex) {
        return buildProblem(HttpStatus.FORBIDDEN, "Droits insuffisants", ex.getMessage(), "insufficient-privileges");
    }

    @ExceptionHandler(ResourceAccessDeniedException.class)
    public ProblemDetail handleAccessDenied(ResourceAccessDeniedException ex) {
        // 403 est parfait pour l'isolation multi-tenant ou unité
        return buildProblem(HttpStatus.FORBIDDEN, "Accès refusé", ex.getMessage(), "resource-access-denied");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Accès refusé : privilèges insuffisants"));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        // 400 est préférable pour une erreur de donnée (ex: nom vide)
        return buildProblem(HttpStatus.BAD_REQUEST, "Règle métier violée", ex.getMessage(), "business-rule-violation");
    }

    // Handler générique pour les autres DomainException non listées
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Erreur métier", ex.getMessage(), "domain-error");
    }

    // --- VALIDATION & SYNTAX EXCEPTIONS ---

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleValidation(IllegalArgumentException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Format invalide", ex.getMessage(), "invalid-format");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        DefaultMessageSourceResolvable::getDefaultMessage,
                        (existing, replacement) -> existing));

        ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, "Validation échouée",
                "Champs invalides", "validation-error");
        problem.setProperty("invalid_params", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleJsonError(HttpMessageNotReadableException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Erreur de lecture JSON",
                "Le format du JSON est invalide (syntaxe incorrecte).", "malformed-json");
    }

    // --- INFRASTRUCTURE EXCEPTIONS ---

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ProblemDetail handleNotFound(jakarta.persistence.EntityNotFoundException ex) {
        return buildProblem(HttpStatus.NOT_FOUND, "Ressource non trouvée", ex.getMessage(), "not-found");
    }

    @ExceptionHandler(KeycloakIdentityException.class)
    public ProblemDetail handleKeycloakException(KeycloakIdentityException ex) {
        HttpStatus status = ex.getMessage().toLowerCase().contains("identifiants") ? HttpStatus.UNAUTHORIZED
                : HttpStatus.INTERNAL_SERVER_ERROR;

        ProblemDetail problem = buildProblem(status, "Erreur Service Identité", ex.getMessage(),
                "identity-service-error");

        if (ex.getCause() != null) {
            problem.setProperty("debug_info", ex.getCause().getMessage());
        }
        return problem;
    }
    

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        // Souvent levé lors d'un doublon d'email si le check préventif a raté
        return buildProblem(HttpStatus.CONFLICT, "Erreur de persistance",
                "Une contrainte d'unicité a été violée (email ou ID déjà existant).", "database-conflict");
    }

    // --- CATCH-ALL FOR UNEXPECTED ERRORS ---

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur Interne",
                "Une erreur inattendue est survenue.", "internal-server-error");
    }
}