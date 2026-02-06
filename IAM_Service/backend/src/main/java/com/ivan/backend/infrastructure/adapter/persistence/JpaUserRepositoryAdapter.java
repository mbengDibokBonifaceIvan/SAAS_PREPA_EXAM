package com.ivan.backend.infrastructure.adapter.persistence;

import com.ivan.backend.domain.entity.User;
import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.valueobject.Email;
import com.ivan.backend.domain.valueobject.UserRole;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository repository;

    @Override
    public User save(User user) {
        // Ta logique de save conservée et isolée
        UserModel entity = repository.findById(user.getId())
                .orElse(new UserModel());

        updateEntityFromDomain(entity, user);

        repository.save(entity);
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return repository.findByEmail(email.value()).map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByRoleAndTenantId(UserRole role, UUID tenantId) {
        return repository.findByRoleAndExternalOrganizationId(role, tenantId).map(this::mapToDomain);
    }

    @Override
    public List<User> findAllByTenantId(UUID tenantId) {
        return repository.findAllByExternalOrganizationId(tenantId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public List<User> findAllByUnitId(UUID unitId) {
        return repository.findAllByExternalUnitId(unitId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public List<User> findAllByUnitIdAndTenantId(UUID unitId, UUID tenantId) {
        return repository.findAllByExternalUnitIdAndExternalOrganizationId(unitId, tenantId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    // --- MAPPING PRIVÉ ---

    private User mapToDomain(UserModel entity) {
        return new User(
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                new Email(entity.getEmail()),
                entity.getExternalOrganizationId(),
                entity.getExternalUnitId(),
                entity.getRole(),
                entity.isEmailVerified(),
                entity.isActive(),
                entity.isMustChangePassword());
    }

    private void updateEntityFromDomain(UserModel model, User domain) {
        model.setUserId(domain.getId());
        model.setFirstName(domain.getFirstName());
        model.setLastName(domain.getLastName());
        model.setEmail(domain.getEmail().value());
        model.setRole(domain.getRole());
        model.setExternalOrganizationId(domain.getTenantId());
        model.setExternalUnitId(domain.getUnitId());
        model.setEmailVerified(domain.isEmailVerified());
        model.setActive(domain.isActive());
        model.setMustChangePassword(domain.isMustChangePassword());
        // On ne fait SURTOUT PAS de setCreatedAt ou setCreatedBy ici.
        // JPA s'en occupe tout seul au moment du save().
    }
}
