package com.erpreal.backend.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, String> {
    List<RoleEntity> findByTenantIdAndEnabledTrueOrderByCodeAsc(String tenantId);
    Optional<RoleEntity> findByTenantIdAndIdAndEnabledTrue(String tenantId, String id);
    Optional<RoleEntity> findByTenantIdAndCodeAndEnabledTrue(String tenantId, String code);
}
