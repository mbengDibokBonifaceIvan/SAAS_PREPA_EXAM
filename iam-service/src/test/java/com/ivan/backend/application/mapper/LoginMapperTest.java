package com.ivan.backend.application.mapper;

import com.ivan.backend.application.dto.LoginResponse;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.AuthToken;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoginMapperTest {

    @Test
    @DisplayName("LoginMapper : devrait mapper User et AuthToken vers LoginResponse")
    void shouldMapToLoginResponse() {
        // GIVEN
        User user = new User(UUID.randomUUID(), "Ivan", "Test", new Email("ivan@test.com"),
                UUID.randomUUID(), null, UserRole.UNIT_MANAGER, true, true, false);
        AuthToken token = new AuthToken("access-123", "refresh-123", 3600L, "Bearer");

        // WHEN
        LoginResponse response = LoginMapper.toResponse(user, token);

        // THEN
        assertAll(
            () -> assertEquals("access-123", response.accessToken()),
            () -> assertEquals("ivan@test.com", response.email()),
            () -> assertEquals("UNIT_MANAGER", response.role()),
            () -> assertFalse(response.mustChangePassword())
        );
    }
}