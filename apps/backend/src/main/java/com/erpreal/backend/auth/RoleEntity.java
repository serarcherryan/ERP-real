package com.erpreal.backend.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "roles")
public class RoleEntity {
    @Id
    public String id;
    @Column(name = "tenant_id", nullable = false)
    public String tenantId;
    @Column(nullable = false)
    public String code;
    @Column(nullable = false)
    public String name;
    public String description;
    @Column(name = "system_builtin", nullable = false)
    public boolean systemBuiltin;
    @Column(nullable = false)
    public boolean enabled;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
