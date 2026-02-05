package com.ivan.backend.application.usecase;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.backend.application.dto.UpdateUserRequest;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.exception.DomainException;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;

import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateUserUseCase {

    private final UserRepository userRepository;

    @Transactional
    public void execute(UUID targetId, String requesterEmail, UpdateUserRequest request) {
        User requester = userRepository.findByEmail(new Email(requesterEmail))
                .orElseThrow(() -> new EntityNotFoundException("Demandeur inconnu"));
        
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new EntityNotFoundException("Cible inconnue"));

        // 1. Sécurité transversale
        validateTenantAccess(requester, target);

        // 2. Mise à jour des informations de profil
        updateBasicInfo(requester, target, request);

        // 3. Mise à jour des privilèges administratifs
        updateAdministrativeInfo(requester, target, request);

        userRepository.save(target);
    }

    private void validateTenantAccess(User requester, User target) {
        if (!target.getTenantId().equals(requester.getTenantId())) {
            throw new DomainException("Accès refusé : Autre organisation.");
        }
    }

    private void updateBasicInfo(User requester, User target, UpdateUserRequest request) {
        // Vérification des droits de modification
        boolean isSelf = target.getId().equals(requester.getId());
        boolean isSuperior = requester.getRole().isHigherThan(target.getRole());

        if (!isSelf && !isSuperior) {
            throw new DomainException("Vous n'avez pas le droit de modifier ce profil.");
        }

        // Fusion intelligente des données
        String firstName = (request.firstName() != null) ? request.firstName() : target.getFirstName();
        String lastName = (request.lastName() != null) ? request.lastName() : target.getLastName();
        
        target.updateProfile(firstName, lastName);
    }

    private void updateAdministrativeInfo(User requester, User target, UpdateUserRequest request) {
        // Si aucun champ admin n'est présent, on sort
        if (request.role() == null && request.unitId() == null) return;

        // Seul l'Owner peut toucher à ça
        if (requester.getRole() != UserRole.CENTER_OWNER) {
            throw new DomainException("Seul le Chef de Centre peut modifier les rôles ou unités.");
        }

        if (request.role() != null) {
            target.changeRole(request.role(), requester.getId());
        }
        
        if (request.unitId() != null) {
            target.assignToUnit(request.unitId());
        }
    }
}