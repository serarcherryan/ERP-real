package com.erpreal.backend.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "role_permissions")
public class RolePermissionEntity {
    @Id
    public String id;
    @Column(name = "tenant_id", nullable = false)
    public String tenantId;
    @Column(name = "role_id", nullable = false)
    public String roleId;
    @Column(name = "permission_id", nullable = false)
    public String permissionId;
    @Column(name = "created_by", nullable = false)
    public String createdBy;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
