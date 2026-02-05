package com.ivan.backend.domain.valueobject;

public record Email(String value) {

    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public Email {
        if (value == null || !value.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Format d'email invalide");
        }
    }
}
