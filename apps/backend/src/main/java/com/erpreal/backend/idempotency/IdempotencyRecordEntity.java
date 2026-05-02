package com.erpreal.backend.idempotency;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecordEntity {
    @Id
    public String id;
    public String tenantId;
    public String facilityId;
    public String idempotencyKey;
    public String action;
    public String requestHash;
    public String resourceType;
    public String resourceId;
    public String status;
    public Instant createdAt;
    public Instant updatedAt;
}
