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
        UserEntity entity = repository.findById(user.getId())
                .orElse(new UserEntity());

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

    private User mapToDomain(UserEntity entity) {
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

    private void updateEntityFromDomain(UserEntity entity, User domain) {
        entity.setUserId(domain.getId());
        entity.setFirstName(domain.getFirstName());
        entity.setLastName(domain.getLastName());
        entity.setEmail(domain.getEmail().value());
        entity.setRole(domain.getRole());
        entity.setExternalOrganizationId(domain.getTenantId());
        entity.setExternalUnitId(domain.getUnitId());
        entity.setEmailVerified(domain.isEmailVerified());
        entity.setActive(domain.isActive());
        entity.setMustChangePassword(domain.isMustChangePassword());
    }
}
