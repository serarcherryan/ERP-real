package com.erpreal.backend.resident;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "resident_health_summaries")
public class ResidentHealthSummaryEntity {
    @Id
    public String id;
    public String tenantId;
    public String facilityId;
    public String residentId;
    public String bloodType;
    public String allergySummaryText;
    public String chronicDiseaseSummaryText;
    public String mobilityLevel;
    public String cognitiveStatus;
    public String dietRequirement;
    public String fallRiskLevel;
    public String emergencyPlan;
    @Version
    public long version;
    public Instant createdAt;
    public Instant updatedAt;

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
