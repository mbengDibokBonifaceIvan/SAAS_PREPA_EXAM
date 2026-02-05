package com.ivan.backend.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.backend.application.dto.ProvisionUserRequest;
import com.ivan.backend.application.port.ProvisionUserInputPort;
import com.ivan.backend.application.util.PasswordGenerator;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.event.UserProvisionedEvent;
import com.ivan.backend.domain.exception.DomainException;
import com.ivan.backend.domain.port.IdentityManagerPort;
import com.ivan.backend.domain.port.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProvisionUserUseCase implements ProvisionUserInputPort {

    private final UserRepository userRepository;
    private final IdentityManagerPort identityManagerPort;
    private final MessagePublisherPort messagePublisher;

    @Override
    @Transactional
    public void execute(ProvisionUserRequest request, String creatorEmail) {
        // 1. Récupérer le créateur
        User creator = userRepository.findByEmail(new Email(creatorEmail))
                .orElseThrow(() -> new DomainException("Action impossible : créateur inconnu."));

        // 2. Utiliser la validation de l'ENTITÉ (On lui passe les infos de la cible)
        // On passe le tenantId du créateur car l'user créé doit être dans le même
        // centre
        creator.validateCanCreate(request.role(), creator.getTenantId(), request.unitId());

        // 3. Préparer l'entité
        User newUser = new User(
                null,
                request.firstName(),
                request.lastName(),
                new Email(request.email()),
                creator.getTenantId(),
                request.unitId(),
                request.role(),
                false, true, true);

        // 4. Identité & Persistance
        String tempPassword = PasswordGenerator.generate();
        identityManagerPort.createIdentity(newUser, tempPassword);
        userRepository.save(newUser);

        // 5. Event
        messagePublisher.publishUserProvisioned(new UserProvisionedEvent(
                newUser.getId(),
                newUser.getEmail().value(), 
                newUser.getRole(),
                newUser.getTenantId(),
                LocalDateTime.now()));
    }
}