package com.ivan.backend.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ivan.backend.domain.valueobject.UserRole;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface SpringDataUserRepository extends JpaRepository<UserModel, UUID> {
    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByRoleAndExternalOrganizationId(UserRole role, UUID externalOrganizationId);
    List<UserModel> findAllByExternalOrganizationId(UUID orgId);
    List<UserModel> findAllByExternalUnitId(UUID unitId);
    List<UserModel> findAllByExternalUnitIdAndExternalOrganizationId(UUID unitId, UUID orgId);
}