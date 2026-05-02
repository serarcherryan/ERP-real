package com.erpreal.backend.resident;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "resident_tags")
public class ResidentTagEntity {
    @Id
    public String id;
    public String tenantId;
    public String facilityId;
    public String residentId;
    public String tagCode;
    public String tagName;
    public String source;
    public String status;
    public Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
