package com.ivan.backend.domain.valueobject;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class EmailTest {

    @Test
    void should_create_email_when_format_is_valid() {
        assertDoesNotThrow(() -> new Email("test@example.com"));
    }

    @Test
    void should_throw_exception_when_format_is_invalid() {
        assertThrows(IllegalArgumentException.class, () -> new Email("invalid-email"));
        assertThrows(IllegalArgumentException.class, () -> new Email("test@.com"));
    }
}