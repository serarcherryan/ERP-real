package com.erpreal.backend.auth;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysUserRepository extends JpaRepository<SysUserEntity, String> {
    Optional<SysUserEntity> findByUsername(String username);
    Optional<SysUserEntity> findByTenantIdAndId(String tenantId, String id);
    Optional<SysUserEntity> findByTenantIdAndUsername(String tenantId, String username);
    List<SysUserEntity> findByTenantIdAndFacilityIdOrderByUsernameAsc(String tenantId, String facilityId);
}
