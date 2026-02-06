package com.ivan.backend.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.backend.application.dto.ProvisionUserRequest;
import com.ivan.backend.application.port.in.ProvisionUserInputPort;
import com.ivan.backend.application.util.PasswordGenerator;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.event.UserProvisionedEvent;
import com.ivan.backend.domain.exception.UserAlreadyExistsException;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.port.out.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisionUserUseCase implements ProvisionUserInputPort {

    private final UserRepository userRepository;
    private final IdentityManagerPort identityManagerPort;
    private final MessagePublisherPort messagePublisher;

    @Override
    @Transactional
    public void execute(ProvisionUserRequest request, String creatorEmail) {
        Email targetEmail = new Email(request.email());

        // 1. Fail-fast : Vérifier si l'utilisateur existe déjà
        if (userRepository.findByEmail(targetEmail).isPresent()) {
            throw new UserAlreadyExistsException(targetEmail.value());
        }

        // 2. Récupérer le créateur (celui qui fait l'action)
        User creator = userRepository.findByEmail(new Email(creatorEmail))
                .orElseThrow(() -> new EntityNotFoundException("Action impossible : créateur inconnu (" + creatorEmail + ")"));

        // 3. LOGIQUE MÉTIER CENTRALISÉE (Entité User)
        // Vérifie le Tenant, la Hiérarchie des rôles et le périmètre de l'unité.
        // Lance ResourceAccessDeniedException ou InsufficientPrivilegesException.
        creator.validateCanCreate(request.role(), creator.getTenantId(), request.unitId());

        // 4. Préparer la nouvelle entité User
        User newUser = new User(
                null,
                request.firstName(),
                request.lastName(),
                targetEmail,
                creator.getTenantId(),
                request.unitId(),
                request.role(),
                false, // emailVerified: attend la première connexion
                true,  // isActive
                true   // mustChangePassword: obligatoire pour un compte provisionné
        );

        // 5. Infrastructure : Identity Provider (Keycloak)
        String tempPassword = PasswordGenerator.generate();
        identityManagerPort.createIdentity(newUser, tempPassword);

        // 6. Persistance locale
        userRepository.save(newUser);

        // 7. Notification / Événement (Asynchrone)
        messagePublisher.publishUserProvisioned(new UserProvisionedEvent(
                newUser.getId(),
                newUser.getEmail().value(), 
                newUser.getRole(),
                newUser.getTenantId(),
                LocalDateTime.now()));

        log.info("Nouvel utilisateur provisionné : {} par {}", targetEmail.value(), creatorEmail);
    }
}