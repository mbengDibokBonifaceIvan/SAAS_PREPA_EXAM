package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.dto.OnboardingResponse;
import com.ivan.backend.application.mapper.OnboardingMapper;
import com.ivan.backend.application.port.in.OnboardingInputPort;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.exception.UserAlreadyExistsException;
import com.ivan.backend.domain.port.out.IdentityManagerPort;
import com.ivan.backend.domain.port.out.MessagePublisherPort;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.service.OnboardingDomainService;
import com.ivan.backend.domain.valueobject.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingUseCase implements OnboardingInputPort{

    private final OnboardingDomainService onboardingDomainService;
    private final IdentityManagerPort identityManagerPort;
    private final MessagePublisherPort messagePublisherPort;
    private final UserRepository userRepository;
    private final OnboardingMapper mapper;

    @Override
    @Transactional
    public OnboardingResponse execute(OnboardingRequest request) {
        // 1. Vérification préalable (Fail-fast)
        if (userRepository.findByEmail(new Email(request.email())).isPresent()) {
            throw new UserAlreadyExistsException(request.email());
        }

        // 2. Initialisation du périmètre (Tenant)
        UUID tenantId = UUID.randomUUID();
        User owner = mapper.toDomain(request, tenantId);

        // 3. Logique Domaine (Validation du nom d'organisation, etc.)
        // Le service de domaine retourne l'événement si tout est OK
        var event = onboardingDomainService.initiateOnboarding(owner, request.organizationName());

        // 4. Persistance locale
        // On sauve l'utilisateur (le futur Owner) avant l'appel externe
        userRepository.save(owner);

        // 5. Création de l'identité (IAM)
        // Si Keycloak échoue, @Transactional annulera le userRepository.save(owner)
        identityManagerPort.createIdentity(owner, request.password());

        // 6. Communication asynchrone
        messagePublisherPort.publishOrganizationRegistered(event);

        log.info("Onboarding réussi pour l'organisation : {} (Owner: {})", 
                 request.organizationName(), owner.getEmail().value());

        return mapper.toResponse(owner);
    }
}