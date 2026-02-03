package com.ivan.backend.presentation.v1.rest;

import com.ivan.backend.domain.exception.AccountLockedException;
import com.ivan.backend.domain.exception.KeycloakIdentityException;
import com.ivan.backend.domain.exception.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_KEY = "error";

    // --- EXCEPTIONS DOMAINE (Format Simple Map) ---

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserExists(UserAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidation(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR_KEY, "Format invalide : " + ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Validation echouée: " + details));
    }

    // --- EXCEPTIONS SÉCURITÉ (Format RFC 7807 ProblemDetail) ---

    @ExceptionHandler(AccountLockedException.class)
    public ProblemDetail handleAccountLocked(AccountLockedException ex) {
        // Code 403 Forbidden car l'accès est refusé à cause du statut du compte
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Compte Verrouillé");
        problemDetail.setType(URI.create("https://api.exams.com/errors/account-locked"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(KeycloakIdentityException.class)
    public ProblemDetail handleKeycloakException(KeycloakIdentityException ex) {
        // Code 401 Unauthorized pour les mauvais identifiants
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        problemDetail.setTitle("Échec de l'authentification");
        problemDetail.setType(URI.create("https://api.exams.com/errors/invalid-credentials"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}