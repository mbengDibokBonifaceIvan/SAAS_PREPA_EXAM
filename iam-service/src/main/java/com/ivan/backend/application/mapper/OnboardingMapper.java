package com.ivan.backend.application.mapper;

import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.dto.OnboardingResponse;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OnboardingMapper {

    public User toDomain(OnboardingRequest request, UUID tenantId) {
        return new User(
            null, // ID sera généré par le domaine
            request.firstName(),
            request.lastName(),
            new Email(request.email()),
            tenantId,
            null, // unitId par défaut à null
            UserRole.CENTER_OWNER,
            false ,// emailVerified par défaut à false
            false, // isActive par défaut à false
            false  // mustChangePassword par défaut à false
        );
    }

    public OnboardingResponse toResponse(User user) {
        return new OnboardingResponse(
            user.getFirstName(),
            user.getLastName(),
            user.isActive(),
            user.getTenantId(),
            user.isMustChangePassword(),
            user.isEmailVerified()
        );
    }
}