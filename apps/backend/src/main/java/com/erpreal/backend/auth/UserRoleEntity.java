package com.erpreal.backend.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_roles")
public class UserRoleEntity {
    @Id
    public String id;
    @Column(name = "tenant_id", nullable = false)
    public String tenantId;
    @Column(name = "user_id", nullable = false)
    public String userId;
    @Column(name = "role_id", nullable = false)
    public String roleId;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
