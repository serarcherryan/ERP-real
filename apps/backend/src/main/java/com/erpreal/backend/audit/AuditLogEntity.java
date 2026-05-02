package com.erpreal.backend.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    @Id
    public String id;
    public String tenantId;
    public String facilityId;
    public String moduleName;
    public String action;
    public String resourceType;
    public String resourceId;
    public String operatorId;
    public String operatorRole;
    public String traceId;
    public String detail;
    public Instant createdAt;
}
