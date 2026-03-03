package com.ivan.backend.application.mapper;

import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.dto.OnboardingResponse;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OnboardingMapperTest {

    private final OnboardingMapper mapper = new OnboardingMapper();

    @Test
    @DisplayName("OnboardingMapper : devrait transformer Request en Domain User")
    void shouldMapRequestToDomain() {
        // GIVEN
        UUID tenantId = UUID.randomUUID();
        OnboardingRequest request = new OnboardingRequest("Ivan", "Dev", "admin@test.com", "Pass123!", "Mon Centre");

        // WHEN
        User user = mapper.toDomain(request, tenantId);

        // THEN
        assertAll(
            () -> assertEquals("Ivan", user.getFirstName()),
            () -> assertEquals("admin@test.com", user.getEmail().value()),
            () -> assertEquals(tenantId, user.getTenantId()),
            () -> assertEquals(UserRole.CENTER_OWNER, user.getRole()),
            () -> assertNotNull(user.getId()) // Vérifie que l'ID est bien null comme prévu
        );
    }

    @Test
    @DisplayName("OnboardingMapper : devrait transformer Domain User en Response")
    void shouldMapDomainToResponse() {
        // GIVEN
        UUID tenantId = UUID.randomUUID();
        User user = new User(UUID.randomUUID(), "Ivan", "Dev", new com.ivan.backend.domain.valueobject.Email("admin@test.com"),
                tenantId, null, UserRole.CENTER_OWNER, true, true, false);

        // WHEN
        OnboardingResponse response = mapper.toResponse(user);

        // THEN
        assertAll(
            () -> assertEquals("Ivan", response.firstName()),
            () -> assertEquals(tenantId, response.externalOrganizationId()),
            () -> assertTrue(response.isActive()),
            () -> assertTrue(response.isEmailVerified())
        );
    }
}