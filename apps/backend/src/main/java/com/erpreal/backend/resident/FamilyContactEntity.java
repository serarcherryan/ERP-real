package com.erpreal.backend.resident;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "family_contacts")
public class FamilyContactEntity {
    @Id
    public String id;
    public String tenantId;
    public String facilityId;
    public String residentId;
    public String name;
    public String relation;
    public String phoneCipher;
    public String phoneHash;
    public String addressCipher;
    public boolean isEmergency;
    public boolean isGuardian;
    public boolean canReceiveNotice;
    public int priority;
    public String status;
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
