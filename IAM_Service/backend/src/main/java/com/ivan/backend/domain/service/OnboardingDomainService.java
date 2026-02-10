package com.ivan.backend.domain.service;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.event.OrganizationRegisteredEvent;
import com.ivan.backend.domain.exception.UserAlreadyExistsException;
import com.ivan.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OnboardingDomainService {
    
    private final UserRepository userRepository;

    public OrganizationRegisteredEvent initiateOnboarding(User owner, String orgName) {
        // Règle métier : On ne peut pas créer une organisation avec un email déjà pris
        if (userRepository.findByEmail(owner.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException(owner.getEmail().value());
        }

        // On retourne l'événement qui servira de preuve de succès
        return new OrganizationRegisteredEvent(
            owner.getTenantId(),
            orgName,
            owner.getId(),
            owner.getEmail().value(),
            owner.getFirstName(),
            owner.getLastName(),
            owner.isEmailVerified()
        );
    }
}