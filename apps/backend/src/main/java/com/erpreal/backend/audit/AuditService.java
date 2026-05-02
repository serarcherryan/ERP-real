package com.erpreal.backend.audit;

import com.erpreal.backend.room.RequestContext;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(
            RequestContext context,
            String moduleName,
            String action,
            String resourceType,
            String resourceId,
            String detail) {
        var entity = new AuditLogEntity();
        entity.id = "audit-" + UUID.randomUUID();
        entity.tenantId = context.tenantId();
        entity.facilityId = context.facilityId();
        entity.moduleName = moduleName;
        entity.action = action;
        entity.resourceType = resourceType;
        entity.resourceId = resourceId;
        entity.operatorId = context.userId();
        entity.operatorRole = context.role();
        entity.traceId = context.traceId();
        entity.detail = detail;
        entity.createdAt = Instant.now();
        repository.save(entity);
    }
}
