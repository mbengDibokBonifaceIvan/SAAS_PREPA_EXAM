package com.ivan.backend.application.usecase;
import com.ivan.backend.application.dto.LoginRequest;
import com.ivan.backend.application.dto.LoginResponse;
import com.ivan.backend.application.mapper.LoginMapper;
import com.ivan.backend.application.port.LoginInputPort;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.port.IdentityGatekeeper;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginUseCase implements LoginInputPort {

    private final IdentityGatekeeper identityGatekeeper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. Authentification auprès de Keycloak (Port de sortie)
        var token = identityGatekeeper.authenticate(request.email(), request.password());

        // 2. Récupération de l'utilisateur local
        User user = userRepository.findByEmail(new Email(request.email()))
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé localement"));

        // 3. Récupération des statuts frais (Email vérifié ? Password à changer ?)
        var providerStatus = identityGatekeeper.getStatus(request.email());

        // 4. LOGIQUE DOMAINE : Synchronisation et Activation
        user.syncValidationStatus(providerStatus.isEmailVerified());
        user.updatePasswordRequirement(providerStatus.mustChangePassword());

        // 5. Persistance de l'état mis à jour
        userRepository.save(user);

        // 6. Retourne la réponse mappée
        return LoginMapper.toResponse(user, token);
    }
}