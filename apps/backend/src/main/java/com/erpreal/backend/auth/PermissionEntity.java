package com.erpreal.backend.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "permissions")
public class PermissionEntity {
    @Id
    public String id;
    @Column(nullable = false, unique = true)
    public String code;
    @Column(nullable = false)
    public String name;
    @Column(name = "module_name", nullable = false)
    public String moduleName;
    @Column(nullable = false)
    public String action;
    @Column(name = "risk_level", nullable = false)
    public String riskLevel;
    @Column(name = "system_builtin", nullable = false)
    public boolean systemBuiltin;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
