package com.erpreal.backend.resident;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ResidentDtos {
    private ResidentDtos() {
    }

    public record ResidentPageResponse(
            List<ResidentListItem> items,
            int page,
            int pageSize,
            long total) {
    }

    public record ResidentListItem(
            String id,
            String tenantId,
            String facilityId,
            String residentNo,
            String name,
            String status,
            String maskedPhone,
            String maskedIdentityNo,
            String admissionStatus,
            String room,
            String bed,
            String careLevel,
            String fallRiskLevel,
            BigDecimal completenessScore,
            List<String> missingFields,
            List<String> tags,
            long version) {
    }

    public record ResidentDetailResponse(
            String id,
            String tenantId,
            String facilityId,
            String departmentId,
            String residentNo,
            String name,
            String preferredName,
            String gender,
            LocalDate birthDate,
            String identityType,
            String maskedIdentityNo,
            String maskedPhone,
            String householdAddress,
            String currentAddress,
            String status,
            AdmissionResponse admission,
            List<FamilyContactResponse> familyContacts,
            FamilyContactResponse primaryContact,
            HealthResponse health,
            String healthSummary,
            List<String> careNeeds,
            List<String> tags,
            BigDecimal completenessScore,
            List<String> missingFields,
            String createdByName,
            String updatedByName,
            Instant lastServiceAt,
            LocalDate nextFollowUpDate,
            long version,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record UpsertResidentRequest(
            @NotBlank @Size(max = 40) String residentNo,
            @NotBlank @Size(max = 40) String name,
            @Size(max = 40) String preferredName,
            @NotBlank String gender,
            @NotNull LocalDate birthDate,
            @NotBlank String identityType,
            @NotBlank String identityNo,
            String phone,
            String householdAddress,
            String currentAddress,
            @NotNull @Valid AdmissionRequest admission,
            @Size(max = 5) List<@Valid FamilyContactRequest> familyContacts,
            @Valid HealthRequest health,
            @Size(max = 500) String healthSummary,
            @Size(max = 20) List<@Size(max = 80) String> careNeeds,
            @Size(max = 20) List<@Size(max = 80) String> tags,
            Long version) {
    }

    public record AdmissionRequest(
            @NotBlank String admissionStatus,
            LocalDate admissionDate,
            String contractNo,
            String zoneId,
            String buildingId,
            String floorId,
            String roomId,
            String bedId,
            String room,
            String bed,
            String nursingZone,
            String careLevel,
            String paymentType,
            String medicalInsuranceType,
            String responsibleSocialWorkerId) {
    }

    public record AdmissionResponse(
            String admissionStatus,
            LocalDate admissionDate,
            String contractNo,
            String zoneId,
            String buildingId,
            String floorId,
            String roomId,
            String bedId,
            String room,
            String bed,
            String nursingZone,
            String careLevel,
            String paymentType,
            String medicalInsuranceType,
            String responsibleSocialWorkerId) {
    }

    public record FamilyContactRequest(
            @NotBlank @Size(max = 40) String name,
            @NotBlank @Size(max = 40) String relation,
            @NotBlank String phone,
            String address,
            boolean isEmergency,
            boolean isGuardian,
            boolean canReceiveNotice,
            @Min(1) @Max(5) int priority) {
    }

    public record FamilyContactResponse(
            String id,
            String name,
            String relation,
            String maskedPhone,
            String address,
            boolean isEmergency,
            boolean isGuardian,
            boolean canReceiveNotice,
            int priority) {
    }

    public record HealthRequest(
            String bloodType,
            @Size(max = 20) List<@Size(max = 80) String> allergyHistory,
            @Size(max = 20) List<@Size(max = 80) String> chronicDiseases,
            String mobilityLevel,
            String cognitiveStatus,
            String dietRequirement,
            String fallRiskLevel,
            String emergencyPlan) {
    }

    public record HealthResponse(
            String bloodType,
            List<String> allergyHistory,
            List<String> chronicDiseases,
            String mobilityLevel,
            String cognitiveStatus,
            String dietRequirement,
            String fallRiskLevel,
            String emergencyPlan) {
    }

    public record VoidResidentRequest(@NotBlank @Size(max = 500) String reason, Long version) {
    }
}
