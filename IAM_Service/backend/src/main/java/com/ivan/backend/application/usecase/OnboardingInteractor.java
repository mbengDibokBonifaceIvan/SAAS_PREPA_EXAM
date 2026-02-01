package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.dto.OnboardingResponse;
import com.ivan.backend.application.mapper.OnboardingMapper;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.port.IdentityManagerPort;
import com.ivan.backend.domain.port.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.service.OnboardingDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingInteractor implements OnboardingUseCase {

    private final OnboardingDomainService onboardingDomainService;
    private final IdentityManagerPort identityManagerPort;
    private final MessagePublisherPort messagePublisherPort;
    private final UserRepository userRepository;
    private final OnboardingMapper mapper;

    @Override
    @Transactional
    public OnboardingResponse execute(OnboardingRequest request) {
        // 1. Générer l'ID de l'organisation (Tenant)
        UUID tenantId = UUID.randomUUID();

        // 2. Transformer la requête en Entité du domaine
        User owner = mapper.toDomain(request, tenantId);

        // 3. Appeler le service de domaine pour valider les règles métier
        var event = onboardingDomainService.initiateOnboarding(owner, request.organizationName());

        // 4. PERSISTANCE : Sauver l'utilisateur en base locale
        userRepository.save(owner);

        // 5. INFRA : Créer l'identité dans Keycloak
        identityManagerPort.createIdentity(owner, request.password());

        // 6. MESSAGERIE : Publier l'événement pour les autres services (Notification, Center Mgmt)
        messagePublisherPort.publishOrganizationRegistered(event);

        // 7. Retourner la réponse
        return mapper.toResponse(owner);
    }
}