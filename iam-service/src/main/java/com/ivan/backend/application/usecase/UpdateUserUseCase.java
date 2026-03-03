package com.ivan.backend.application.usecase;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.backend.application.dto.UpdateUserRequest;
import com.ivan.backend.application.port.in.UpdateUserInputPort;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.exception.InsufficientPrivilegesException;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;

import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateUserUseCase implements UpdateUserInputPort {

    private final UserRepository userRepository;
    private final IdentityManagerPort identityManagerPort;

    @Override
    @Transactional
    public void execute(UUID targetId, String requesterEmail, UpdateUserRequest request) {
        User requester = userRepository.findByEmail(new Email(requesterEmail))
                .orElseThrow(() -> new EntityNotFoundException("Demandeur inconnu : " + requesterEmail));

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new EntityNotFoundException("Cible inconnue (ID: " + targetId + ")"));

        // 1. Validation de sécurité centralisée dans le Domaine
        // Gère l'isolation du centre, l'unité et la hiérarchie.
        requester.checkCanManage(target);

        // 2. Mise à jour des informations de base (Profil)
        updateProfileInfo(target, request);

        // 3. Mise à jour administrative (Rôles & Unités)
        if (request.role() != null || request.unitId() != null) {
            handleAdministrativeUpdates(requester, target, request);
        }

        userRepository.save(target);
        log.info("Mise à jour réussie de l'utilisateur {} par {}", target.getEmail().value(), requesterEmail);
    }

    private void updateProfileInfo(User target, UpdateUserRequest request) {
        String firstName = (request.firstName() != null) ? request.firstName() : target.getFirstName();
        String lastName = (request.lastName() != null) ? request.lastName() : target.getLastName();

        // validateProfile (DomainException si vide)
        target.updateProfile(firstName, lastName);
    }

    private void handleAdministrativeUpdates(User requester, User target, UpdateUserRequest request) {
        // Seul l'Owner a le droit de modifier les structures
        if (requester.getRole() != UserRole.CENTER_OWNER) {
            throw new InsufficientPrivilegesException("Seul le Chef de Centre peut modifier les rôles ou les unités.");
        }

        // Changement de Rôle
        if (request.role() != null) {
            target.changeRole(request.role(), requester.getId());
            // Synchronisation avec Keycloak
            identityManagerPort.updateUserRole(target.getEmail().value(), request.role().name());
        }

        // Changement d'Unité (Sous-centre)
        if (request.unitId() != null) {
            target.assignToUnit(request.unitId());
        }
    }
}