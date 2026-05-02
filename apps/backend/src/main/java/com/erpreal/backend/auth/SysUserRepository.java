package com.erpreal.backend.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysUserRepository extends JpaRepository<SysUserEntity, String> {
    Optional<SysUserEntity> findByUsername(String username);
}
