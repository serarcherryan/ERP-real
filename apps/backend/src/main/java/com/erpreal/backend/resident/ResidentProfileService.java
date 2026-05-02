package com.erpreal.backend.resident;

import com.erpreal.backend.audit.AuditService;
import com.erpreal.backend.auth.PermissionCodes;
import com.erpreal.backend.auth.PermissionService;
import com.erpreal.backend.auth.SysUserRepository;
import com.erpreal.backend.common.ApiException;
import com.erpreal.backend.idempotency.IdempotencyRecordEntity;
import com.erpreal.backend.idempotency.IdempotencyRecordRepository;
import com.erpreal.backend.room.RequestContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResidentProfileService {
    private static final Logger log = LoggerFactory.getLogger(ResidentProfileService.class);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ResidentProfileRepository residents;
    private final FamilyContactRepository familyContacts;
    private final ResidentHealthSummaryRepository healthSummaries;
    private final ResidentTagRepository tags;
    private final IdempotencyRecordRepository idempotencyRecords;
    private final SysUserRepository users;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final SensitiveFieldCodec codec;
    private final ObjectMapper mapper;

    public ResidentProfileService(
            ResidentProfileRepository residents,
            FamilyContactRepository familyContacts,
            ResidentHealthSummaryRepository healthSummaries,
            ResidentTagRepository tags,
            IdempotencyRecordRepository idempotencyRecords,
            SysUserRepository users,
            PermissionService permissionService,
            AuditService auditService,
            SensitiveFieldCodec codec,
            ObjectMapper mapper) {
        this.residents = residents;
        this.familyContacts = familyContacts;
        this.healthSummaries = healthSummaries;
        this.tags = tags;
        this.idempotencyRecords = idempotencyRecords;
        this.users = users;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.codec = codec;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ResidentDtos.ResidentPageResponse list(
            RequestContext context, String keyword, String status, int page, int pageSize) {
        assertCanRead(context);
        var safePage = Math.max(page, 1);
        var safePageSize = Math.min(Math.max(pageSize, 1), 100);
        var pageable = PageRequest.of(safePage - 1, safePageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        var normalizedStatus = blankToNull(status);
        var normalizedKeyword = blankToNull(keyword);
        var result = normalizedKeyword == null
                ? residents.searchWithoutKeyword(context.tenantId(), context.facilityId(), normalizedStatus, pageable)
                : residents.search(
                        context.tenantId(),
                        context.facilityId(),
                        normalizedStatus,
                        normalizedKeyword,
                        pageable);
        var items = result.getContent().stream().map(item -> toListItem(context, item)).toList();
        return new ResidentDtos.ResidentPageResponse(items, safePage, safePageSize, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ResidentDtos.ResidentDetailResponse get(RequestContext context, String residentId) {
        assertCanRead(context);
        var resident = findResident(context, residentId);
        var response = toDetail(context, resident);
        if (canReadSensitive(context)) {
            audit("sensitive-read", context, resident.id, "resident detail sensitive fields viewed");
        }
        return response;
    }

    @Transactional
    public ResidentDtos.ResidentDetailResponse create(RequestContext context, ResidentDtos.UpsertResidentRequest request) {
        assertCanCreate(context);
        var requestHash = hashRequest(request);
        var idempotencyRecord = reserveIdempotency(context, "resident.create", requestHash);
        if (idempotencyRecord != null && "completed".equals(idempotencyRecord.status)) {
            return get(context, idempotencyRecord.resourceId);
        }

        var identityHash = codec.hash(request.identityNo());
        residents.findByTenantIdAndResidentNo(context.tenantId(), request.residentNo()).ifPresent(existing -> {
            throw duplicated();
        });
        residents.findByTenantIdAndIdentityNoHash(context.tenantId(), identityHash).ifPresent(existing -> {
            throw duplicated();
        });

        var resident = new ResidentProfileEntity();
        resident.id = "resident-" + UUID.randomUUID();
        resident.tenantId = context.tenantId();
        resident.facilityId = context.facilityId();
        resident.departmentId = "dept-care";
        resident.status = "Active";
        resident.createdBy = context.userId();
        applyResidentFields(context, resident, request, true);

        try {
            var saved = residents.save(resident);
            replaceChildren(context, saved, request);
            completeIdempotency(idempotencyRecord, "resident", saved.id);
            audit("create", context, saved.id, "resident profile created");
            return toDetail(context, saved);
        } catch (DataIntegrityViolationException exception) {
            throw duplicated();
        }
    }

    @Transactional
    public ResidentDtos.ResidentDetailResponse update(
            RequestContext context, String residentId, ResidentDtos.UpsertResidentRequest request) {
        assertCanUpdate(context);
        var resident = findResident(context, residentId);
        if (request.version() == null || request.version() != resident.version) {
            throw new ApiException("RESIDENT_PROFILE_VERSION_CONFLICT", HttpStatus.CONFLICT,
                    "Resident profile version conflict");
        }
        residents.findByTenantIdAndResidentNo(context.tenantId(), request.residentNo())
                .filter(existing -> !existing.id.equals(resident.id))
                .ifPresent(existing -> {
                    throw duplicated();
                });
        var newIdentityHash = codec.hash(request.identityNo());
        if (!Objects.equals(newIdentityHash, resident.identityNoHash)) {
            residents.findByTenantIdAndIdentityNoHash(context.tenantId(), newIdentityHash).ifPresent(existing -> {
                throw duplicated();
            });
        }
        applyResidentFields(context, resident, request, false);
        try {
            var saved = residents.save(resident);
            replaceChildren(context, saved, request);
            audit("update", context, saved.id, "resident profile updated");
            return toDetail(context, saved);
        } catch (DataIntegrityViolationException exception) {
            throw duplicated();
        }
    }

    @Transactional
    public ResidentDtos.ResidentDetailResponse voidResident(
            RequestContext context, String residentId, ResidentDtos.VoidResidentRequest request) {
        assertCanVoid(context);
        var resident = findResident(context, residentId);
        if (request.version() != null && request.version() != resident.version) {
            throw new ApiException("RESIDENT_PROFILE_VERSION_CONFLICT", HttpStatus.CONFLICT,
                    "Resident profile version conflict");
        }
        resident.status = "Archived";
        resident.archivedAt = Instant.now();
        resident.archivedReason = request.reason();
        resident.updatedBy = context.userId();
        var saved = residents.save(resident);
        audit("void", context, saved.id, "resident profile archived");
        return toDetail(context, saved);
    }

    private void applyResidentFields(
            RequestContext context,
            ResidentProfileEntity resident,
            ResidentDtos.UpsertResidentRequest request,
            boolean creating) {
        resident.updatedBy = context.userId();
        resident.residentNo = request.residentNo();
        resident.name = request.name();
        resident.preferredName = request.preferredName();
        resident.gender = request.gender();
        resident.birthDate = request.birthDate();
        resident.identityType = request.identityType();
        resident.identityNoCipher = codec.encrypt(request.identityNo());
        resident.identityNoHash = codec.hash(request.identityNo());
        resident.phoneCipher = codec.encrypt(request.phone());
        resident.phoneHash = codec.hash(request.phone());
        resident.householdAddressCipher = codec.encrypt(request.householdAddress());
        resident.currentAddressCipher = codec.encrypt(request.currentAddress());
        resident.admissionStatus = request.admission().admissionStatus();
        resident.admissionDate = request.admission().admissionDate();
        resident.contractNo = request.admission().contractNo();
        resident.zoneId = request.admission().zoneId();
        resident.buildingId = request.admission().buildingId();
        resident.floorId = request.admission().floorId();
        resident.roomId = request.admission().roomId();
        resident.bedId = request.admission().bedId();
        resident.roomLabel = request.admission().room();
        resident.bedLabel = request.admission().bed();
        resident.livingLocationLabel = request.admission().room();
        resident.nursingZone = request.admission().nursingZone();
        resident.careLevel = request.admission().careLevel();
        resident.paymentType = request.admission().paymentType();
        resident.medicalInsuranceType = request.admission().medicalInsuranceType();
        resident.responsibleSocialWorkerId = request.admission().responsibleSocialWorkerId();
        resident.healthSummary = request.healthSummary();
        resident.careNeedsText = writeList(request.careNeeds());
        var missingFields = missingFields(request);
        resident.missingFieldsText = writeList(missingFields);
        resident.completenessScore = completenessScore(missingFields);
        if (creating && resident.status == null) {
            resident.status = "Active";
        }
    }

    private void replaceChildren(
            RequestContext context,
            ResidentProfileEntity resident,
            ResidentDtos.UpsertResidentRequest request) {
        familyContacts.deleteByTenantIdAndFacilityIdAndResidentId(context.tenantId(), context.facilityId(), resident.id);
        familyContacts.flush();
        var contactEntities = nullToEmptyContacts(request.familyContacts()).stream()
                .sorted(Comparator.comparingInt(ResidentDtos.FamilyContactRequest::priority))
                .map(contact -> {
                    var entity = new FamilyContactEntity();
                    entity.id = "family-contact-" + UUID.randomUUID();
                    entity.tenantId = context.tenantId();
                    entity.facilityId = context.facilityId();
                    entity.residentId = resident.id;
                    entity.name = contact.name();
                    entity.relation = contact.relation();
                    entity.phoneCipher = codec.encrypt(contact.phone());
                    entity.phoneHash = codec.hash(contact.phone());
                    entity.addressCipher = codec.encrypt(contact.address());
                    entity.isEmergency = contact.isEmergency();
                    entity.isGuardian = contact.isGuardian();
                    entity.canReceiveNotice = contact.canReceiveNotice();
                    entity.priority = contact.priority() == 0 ? 1 : contact.priority();
                    entity.status = "Active";
                    return entity;
                })
                .toList();
        familyContacts.saveAll(contactEntities);

        healthSummaries.deleteByTenantIdAndFacilityIdAndResidentId(context.tenantId(), context.facilityId(), resident.id);
        healthSummaries.flush();
        if (request.health() != null) {
            var health = new ResidentHealthSummaryEntity();
            health.id = "resident-health-" + UUID.randomUUID();
            health.tenantId = context.tenantId();
            health.facilityId = context.facilityId();
            health.residentId = resident.id;
            health.bloodType = request.health().bloodType();
            health.allergySummaryText = writeList(request.health().allergyHistory());
            health.chronicDiseaseSummaryText = writeList(request.health().chronicDiseases());
            health.mobilityLevel = request.health().mobilityLevel();
            health.cognitiveStatus = request.health().cognitiveStatus();
            health.dietRequirement = request.health().dietRequirement();
            health.fallRiskLevel = request.health().fallRiskLevel();
            health.emergencyPlan = request.health().emergencyPlan();
            healthSummaries.save(health);
        }

        tags.deleteByTenantIdAndFacilityIdAndResidentId(context.tenantId(), context.facilityId(), resident.id);
        tags.flush();
        tags.saveAll(nullToEmpty(request.tags()).stream().distinct().map(tag -> {
            var entity = new ResidentTagEntity();
            entity.id = "resident-tag-" + UUID.randomUUID();
            entity.tenantId = context.tenantId();
            entity.facilityId = context.facilityId();
            entity.residentId = resident.id;
            entity.tagCode = tag;
            entity.tagName = tag;
            entity.source = "manual";
            entity.status = "Active";
            return entity;
        }).toList());
    }

    private ResidentDtos.ResidentListItem toListItem(RequestContext context, ResidentProfileEntity resident) {
        var health = healthSummaries.findByTenantIdAndFacilityIdAndResidentId(
                context.tenantId(), context.facilityId(), resident.id).orElse(null);
        return new ResidentDtos.ResidentListItem(
                resident.id,
                resident.tenantId,
                resident.facilityId,
                resident.residentNo,
                resident.name,
                resident.status,
                codec.maskPhone(codec.decrypt(resident.phoneCipher)),
                codec.maskIdentityNo(codec.decrypt(resident.identityNoCipher)),
                resident.admissionStatus,
                firstNonBlank(resident.livingLocationLabel, resident.roomLabel),
                resident.bedLabel,
                resident.careLevel,
                health == null ? null : health.fallRiskLevel,
                resident.completenessScore,
                readList(resident.missingFieldsText),
                tagNames(context, resident.id),
                resident.version);
    }

    private ResidentDtos.ResidentDetailResponse toDetail(RequestContext context, ResidentProfileEntity resident) {
        var canReadSensitive = canReadSensitive(context);
        var identityNo = codec.decrypt(resident.identityNoCipher);
        var phone = codec.decrypt(resident.phoneCipher);
        var contacts = familyContacts
                .findByTenantIdAndFacilityIdAndResidentIdAndStatusOrderByPriorityAsc(
                        context.tenantId(), context.facilityId(), resident.id, "Active")
                .stream()
                .map(contact -> new ResidentDtos.FamilyContactResponse(
                        contact.id,
                        contact.name,
                        contact.relation,
                        canReadSensitive ? codec.decrypt(contact.phoneCipher) : codec.maskPhone(codec.decrypt(contact.phoneCipher)),
                        canReadSensitive ? codec.decrypt(contact.addressCipher) : null,
                        contact.isEmergency,
                        contact.isGuardian,
                        contact.canReceiveNotice,
                        contact.priority))
                .toList();
        var health = healthSummaries.findByTenantIdAndFacilityIdAndResidentId(
                        context.tenantId(), context.facilityId(), resident.id)
                .map(item -> new ResidentDtos.HealthResponse(
                        item.bloodType,
                        readList(item.allergySummaryText),
                        readList(item.chronicDiseaseSummaryText),
                        item.mobilityLevel,
                        item.cognitiveStatus,
                        item.dietRequirement,
                        item.fallRiskLevel,
                        item.emergencyPlan))
                .orElse(null);
        return new ResidentDtos.ResidentDetailResponse(
                resident.id,
                resident.tenantId,
                resident.facilityId,
                resident.departmentId,
                resident.residentNo,
                resident.name,
                resident.preferredName,
                resident.gender,
                resident.birthDate,
                resident.identityType,
                canReadSensitive ? identityNo : codec.maskIdentityNo(identityNo),
                canReadSensitive ? phone : codec.maskPhone(phone),
                canReadSensitive ? codec.decrypt(resident.householdAddressCipher) : null,
                canReadSensitive ? codec.decrypt(resident.currentAddressCipher) : null,
                resident.status,
                new ResidentDtos.AdmissionResponse(
                        resident.admissionStatus,
                        resident.admissionDate,
                        resident.contractNo,
                        resident.zoneId,
                        resident.buildingId,
                        resident.floorId,
                        resident.roomId,
                        resident.bedId,
                        firstNonBlank(resident.livingLocationLabel, resident.roomLabel),
                        resident.bedLabel,
                        resident.nursingZone,
                        resident.careLevel,
                        resident.paymentType,
                        resident.medicalInsuranceType,
                        resident.responsibleSocialWorkerId),
                contacts,
                contacts.isEmpty() ? null : contacts.getFirst(),
                health,
                resident.healthSummary,
                readList(resident.careNeedsText),
                tagNames(context, resident.id),
                resident.completenessScore,
                readList(resident.missingFieldsText),
                displayNameFor(context, resident.createdBy),
                displayNameFor(context, resident.updatedBy),
                resident.lastServiceAt,
                resident.nextFollowUpDate,
                resident.version,
                resident.createdAt,
                resident.updatedAt);
    }

    private String displayNameFor(RequestContext context, String userId) {
        if (isBlank(userId)) {
            return null;
        }
        if (Objects.equals(context.userId(), userId)) {
            return context.displayName();
        }
        return users.findById(userId)
                .map(user -> firstNonBlank(user.getDisplayName(), userId))
                .orElse(userId);
    }

    private ResidentProfileEntity findResident(RequestContext context, String residentId) {
        return residents.findByIdAndTenantIdAndFacilityId(residentId, context.tenantId(), context.facilityId())
                .orElseThrow(() -> new ApiException("RESIDENT_PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Resident profile not found"));
    }

    private List<String> missingFields(ResidentDtos.UpsertResidentRequest request) {
        var missing = new ArrayList<String>();
        if (isBlank(request.phone())) missing.add("phone");
        if (request.familyContacts() == null || request.familyContacts().isEmpty()) missing.add("familyContacts");
        if (request.health() == null) missing.add("health");
        if (isBlank(request.admission().roomId())) missing.add("roomId");
        if (isBlank(request.admission().contractNo())) missing.add("contractNo");
        return missing;
    }

    private BigDecimal completenessScore(List<String> missingFields) {
        var score = 100 - missingFields.size() * 12;
        return BigDecimal.valueOf(Math.max(score, 40));
    }

    private List<String> tagNames(RequestContext context, String residentId) {
        return tags.findByTenantIdAndFacilityIdAndResidentIdAndStatusOrderByTagNameAsc(
                        context.tenantId(), context.facilityId(), residentId, "Active")
                .stream()
                .map(tag -> tag.tagName)
                .toList();
    }

    private void assertCanRead(RequestContext context) {
        permissionService.require(context, PermissionCodes.RESIDENT_PROFILE_READ, "RESIDENT_PROFILE_FORBIDDEN",
                "Current user cannot read resident profile");
    }

    private void assertCanCreate(RequestContext context) {
        permissionService.require(context, PermissionCodes.RESIDENT_PROFILE_CREATE, "RESIDENT_PROFILE_FORBIDDEN",
                "Current user cannot create resident profile");
    }

    private void assertCanUpdate(RequestContext context) {
        permissionService.require(context, PermissionCodes.RESIDENT_PROFILE_UPDATE, "RESIDENT_PROFILE_FORBIDDEN",
                "Current user cannot update resident profile");
    }

    private void assertCanVoid(RequestContext context) {
        permissionService.require(context, PermissionCodes.RESIDENT_PROFILE_DELETE, "RESIDENT_PROFILE_FORBIDDEN",
                "Current user cannot delete resident profile");
    }

    private boolean canReadSensitive(RequestContext context) {
        return List.of("social-worker-supervisor", "department-manager", "admin").contains(context.role());
    }

    private ApiException duplicated() {
        return new ApiException("RESIDENT_PROFILE_DUPLICATED", HttpStatus.CONFLICT,
                "Resident profile residentNo or identityNo already exists");
    }

    private void audit(String action, RequestContext context, String residentId, String detail) {
        auditService.record(context, "resident-profile", action, "resident", residentId, detail);
        log.info("resident_profile_audit action={} residentId={} tenantId={} facilityId={} role={}",
                action, residentId, context.tenantId(), context.facilityId(), context.role());
    }

    private IdempotencyRecordEntity reserveIdempotency(RequestContext context, String action, String requestHash) {
        if (context.idempotencyKey() == null) {
            return null;
        }
        var existing = idempotencyRecords.findByTenantIdAndFacilityIdAndActionAndIdempotencyKey(
                context.tenantId(), context.facilityId(), action, context.idempotencyKey());
        if (existing.isPresent()) {
            var record = existing.get();
            if (!record.requestHash.equals(requestHash)) {
                throw new ApiException("IDEMPOTENCY_KEY_REUSED", HttpStatus.CONFLICT,
                        "Idempotency key was used with different request body");
            }
            if (!"completed".equals(record.status)) {
                throw new ApiException("IDEMPOTENCY_IN_PROGRESS", HttpStatus.CONFLICT,
                        "Idempotent request is still in progress");
            }
            return record;
        }
        var record = new IdempotencyRecordEntity();
        record.id = "idempotency-" + UUID.randomUUID();
        record.tenantId = context.tenantId();
        record.facilityId = context.facilityId();
        record.idempotencyKey = context.idempotencyKey();
        record.action = action;
        record.requestHash = requestHash;
        record.status = "pending";
        record.createdAt = Instant.now();
        record.updatedAt = record.createdAt;
        try {
            return idempotencyRecords.save(record);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException("IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT, "Idempotency key conflict");
        }
    }

    private void completeIdempotency(IdempotencyRecordEntity record, String resourceType, String resourceId) {
        if (record == null) {
            return;
        }
        record.resourceType = resourceType;
        record.resourceId = resourceId;
        record.status = "completed";
        record.updatedAt = Instant.now();
        idempotencyRecords.save(record);
    }

    private String hashRequest(ResidentDtos.UpsertResidentRequest request) {
        try {
            return sha256(mapper.writeValueAsString(request));
        } catch (Exception exception) {
            throw new ApiException("REQUEST_HASH_FAILED", HttpStatus.BAD_REQUEST, "Request hash failed");
        }
    }

    private String writeList(List<String> values) {
        try {
            return mapper.writeValueAsString(nullToEmpty(values));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot serialize resident list field", exception);
        }
    }

    private List<String> readList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(value, STRING_LIST);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static List<ResidentDtos.FamilyContactRequest> nullToEmptyContacts(
            List<ResidentDtos.FamilyContactRequest> values) {
        return values == null ? List.of() : values;
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
