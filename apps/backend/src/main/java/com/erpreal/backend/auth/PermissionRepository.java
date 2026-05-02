package com.erpreal.backend.auth;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<PermissionEntity, String> {
    List<PermissionEntity> findAllByOrderByCodeAsc();
    List<PermissionEntity> findByCodeIn(Collection<String> codes);
}
