package com.ivan.backend.application.usecase;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import com.ivan.backend.application.port.in.SearchUserInputPort;
import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchUserUseCase implements SearchUserInputPort {

    private final UserRepository userRepository;

    /**
     * Récupère l'annuaire selon le rôle du demandeur.
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> getDirectory(String requesterEmail, UUID optionalUnitId) {
        User requester = findUserByEmail(requesterEmail);

        return switch (requester.getRole()) {

            // Le chef de centre voit tout son centre, ou filtre par unité s'il le souhaite
            case CENTER_OWNER -> (optionalUnitId != null)
                    ? userRepository.findAllByUnitIdAndTenantId(optionalUnitId, requester.getTenantId())
                    : userRepository.findAllByTenantId(requester.getTenantId());

            // Le manager est restreint à sa propre unité par défaut
            case UNIT_MANAGER -> userRepository.findAllByUnitId(requester.getUnitId());

            // Un membre du staff ou candidat ne voit que lui-même
            default -> List.of(requester);

        };
    }

    /**
     * Récupère un utilisateur spécifique par son ID avec vérification des droits.
     */
    @Override
    @Transactional(readOnly = true)
    public User getUserById(UUID id, String requesterEmail) {
        User requester = findUserByEmail(requesterEmail);
        User target = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID : " + id));

        // Utilisation de la logique centralisée dans l'entité User
        // Lance ResourceAccessDeniedException (403) si le périmètre est mauvais
        requester.checkCanManage(target);

        return target;
    }

    /**
     * Récupère le profil propre de l'utilisateur connecté.
     */
    @Override
    @Transactional(readOnly = true)
    public User getUserProfile(String email) {
        return findUserByEmail(email);
    }

    /**
     * Helper privé pour la récupération par email
     */
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(new Email(email))
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur inconnu : " + email));
    }
}