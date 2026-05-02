package com.erpreal.backend.auth;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, String> {
    List<RolePermissionEntity> findByTenantIdAndRoleId(String tenantId, String roleId);
    List<RolePermissionEntity> findByTenantIdAndRoleIdIn(String tenantId, List<String> roleIds);
    void deleteByTenantIdAndRoleId(String tenantId, String roleId);
}
