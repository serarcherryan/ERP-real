package com.erpreal.backend.resident;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "resident_profiles")
public class ResidentProfileEntity {
    @Id
    public String id;
    public String tenantId;
    public String facilityId;
    public String departmentId;
    public String residentNo;
    public String name;
    public String preferredName;
    public String gender;
    public LocalDate birthDate;
    public String identityType;
    public String identityNoCipher;
    public String identityNoHash;
    public String phoneCipher;
    public String phoneHash;
    public String householdAddressCipher;
    public String currentAddressCipher;
    public String status;
    public String admissionStatus;
    public LocalDate admissionDate;
    public String contractNo;
    public String zoneId;
    public String buildingId;
    public String floorId;
    public String roomId;
    public String bedId;
    public String roomLabel;
    public String bedLabel;
    public String nursingZone;
    public String careLevel;
    public String paymentType;
    public String medicalInsuranceType;
    public String responsibleSocialWorkerId;
    public String healthSummary;
    public String careNeedsText;
    public BigDecimal completenessScore;
    public String missingFieldsText;
    public Instant lastServiceAt;
    public LocalDate nextFollowUpDate;
    public String createdBy;
    public String updatedBy;
    public Instant createdAt;
    public Instant updatedAt;
    public Instant archivedAt;
    public String archivedReason;
    public String livingLocationLabel;
    @Version
    public long version;

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
