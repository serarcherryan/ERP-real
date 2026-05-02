package com.erpreal.backend.auth;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, String> {
    List<UserRoleEntity> findByTenantIdAndUserId(String tenantId, String userId);
    List<UserRoleEntity> findByTenantIdAndRoleId(String tenantId, String roleId);
    void deleteByTenantIdAndUserId(String tenantId, String userId);
}
