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
            UserRole.CENTER_OWNER
        );
    }

    public OnboardingResponse toResponse(User user) {
        return new OnboardingResponse(
            user.getFirstName(),
            user.getLastName(),
            false, // isActive (Pending)
            user.getTenantId(),
            true   // mustChangePassword
        );
    }
}