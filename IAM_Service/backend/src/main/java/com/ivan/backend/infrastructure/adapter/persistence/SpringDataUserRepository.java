package com.ivan.backend.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ivan.backend.domain.valueobject.UserRole;

import java.util.UUID;
import java.util.Optional;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByRoleAndExternalOrganizationId(UserRole role, UUID externalOrganizationId);
}