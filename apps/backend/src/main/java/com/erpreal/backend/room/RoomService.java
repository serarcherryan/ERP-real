package com.erpreal.backend.room;

import com.erpreal.backend.audit.AuditService;
import com.erpreal.backend.common.ApiException;
import com.erpreal.backend.idempotency.IdempotencyRecordEntity;
import com.erpreal.backend.idempotency.IdempotencyRecordRepository;
import com.erpreal.backend.resident.ResidentProfileRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {
    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final HousingZoneRepository zones;
    private final BuildingRepository buildings;
    private final FloorRepository floors;
    private final RoomRepository rooms;
    private final BedAssignmentRepository bedAssignments;
    private final ResidentProfileRepository residents;
    private final IdempotencyRecordRepository idempotencyRecords;
    private final AuditService auditService;
    private final String onlyActiveZoneName;

    public RoomService(
            HousingZoneRepository zones,
            BuildingRepository buildings,
            FloorRepository floors,
            RoomRepository rooms,
            BedAssignmentRepository bedAssignments,
            ResidentProfileRepository residents,
            IdempotencyRecordRepository idempotencyRecords,
            AuditService auditService,
            @Value("${erp.room.only-active-zone-name}") String onlyActiveZoneName) {
        this.zones = zones;
        this.buildings = buildings;
        this.floors = floors;
        this.rooms = rooms;
        this.bedAssignments = bedAssignments;
        this.residents = residents;
        this.idempotencyRecords = idempotencyRecords;
        this.auditService = auditService;
        this.onlyActiveZoneName = onlyActiveZoneName;
    }

    @Transactional(readOnly = true)
    public RoomDtos.RoomTreeResponse tree(RequestContext context) {
        var zoneList = activeZones(context);
        var buildingList = buildings.findByTenantIdAndFacilityId(context.tenantId(), context.facilityId());
        var floorList = floors.findByTenantIdAndFacilityId(context.tenantId(), context.facilityId());
        var roomList = rooms.findByTenantIdAndFacilityId(context.tenantId(), context.facilityId());

        var zoneNodes = zoneList.stream()
                .map(zone -> new RoomDtos.ZoneNode(
                        zone.id,
                        zone.name,
                        buildingList.stream()
                                .filter(building -> building.zoneId.equals(zone.id))
                                .sorted(Comparator.comparingInt(building -> building.sortOrder))
                                .map(building -> new RoomDtos.BuildingNode(
                                        building.id,
                                        building.name,
                                        floorList.stream()
                                                .filter(floor -> floor.buildingId.equals(building.id))
                                                .sorted(Comparator.comparingInt(floor -> floor.floorNo))
                                                .map(floor -> new RoomDtos.FloorNode(
                                                        floor.id,
                                                        floor.name,
                                                        roomList.stream()
                                                                .filter(room -> room.floorId.equals(floor.id))
                                                                .sorted(Comparator.comparing(room -> room.roomNo))
                                                                .map(room -> toResponse(zone, building, floor, room))
                                                                .toList()))
                                                .toList()))
                                .toList()))
                .toList();

        return new RoomDtos.RoomTreeResponse(zoneNodes);
    }

    @Transactional(readOnly = true)
    public List<RoomDtos.RoomResponse> list(
            RequestContext context, String zoneId, String buildingId, String floorId, RoomStatus status) {
        var zoneList = activeZones(context);
        var buildingList = buildings.findByTenantIdAndFacilityId(context.tenantId(), context.facilityId());
        var floorList = floors.findByTenantIdAndFacilityId(context.tenantId(), context.facilityId());

        return rooms.findByTenantIdAndFacilityId(context.tenantId(), context.facilityId()).stream()
                .filter(room -> zoneId == null || room.zoneId.equals(zoneId))
                .filter(room -> buildingId == null || room.buildingId.equals(buildingId))
                .filter(room -> floorId == null || room.floorId.equals(floorId))
                .filter(room -> status == null || room.status == status)
                .map(room -> toResponse(
                        findZone(zoneList, room.zoneId),
                        findBuilding(buildingList, room.buildingId),
                        findFloor(floorList, room.floorId),
                        room))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomDtos.RoomResponse get(RequestContext context, String roomId) {
        var room = findRoom(context, roomId);
        return toResponse(
                findZone(activeZones(context), room.zoneId),
                findBuilding(buildings.findByTenantIdAndFacilityId(context.tenantId(), context.facilityId()), room.buildingId),
                findFloor(floors.findByTenantIdAndFacilityId(context.tenantId(), context.facilityId()), room.floorId),
                room);
    }

    @Transactional
    public RoomDtos.RoomResponse create(RequestContext context, RoomDtos.UpsertRoomRequest request) {
        assertMaintainer(context);
        assertZoneScope(context, request.zoneId());
        assertHierarchy(context, request.zoneId(), request.buildingId(), request.floorId());
        var requestHash = hashRoomRequest(request);
        var idempotencyRecord = reserveIdempotency(context, "room.create", requestHash);
        if (idempotencyRecord != null && "completed".equals(idempotencyRecord.status)) {
            return get(context, idempotencyRecord.resourceId);
        }
        if (rooms.existsByTenantIdAndFacilityIdAndFloorIdAndRoomNo(
                context.tenantId(), context.facilityId(), request.floorId(), request.roomNo())) {
            throw duplicatedRoom();
        }

        var room = new RoomEntity();
        room.id = "room-" + UUID.randomUUID();
        room.tenantId = context.tenantId();
        room.facilityId = context.facilityId();
        room.zoneId = request.zoneId();
        room.buildingId = request.buildingId();
        room.floorId = request.floorId();
        room.roomNo = request.roomNo();
        room.displayName = request.displayName();
        room.capacity = request.capacity();
        room.occupiedCount = 0;
        room.status = request.status();

        try {
            var saved = rooms.save(room);
            completeIdempotency(idempotencyRecord, "room", saved.id);
            audit("create", context, saved.id, "room created");
            return get(context, saved.id);
        } catch (DataIntegrityViolationException exception) {
            throw duplicatedRoom();
        }
    }

    @Transactional
    public RoomDtos.RoomResponse update(RequestContext context, String roomId, RoomDtos.UpsertRoomRequest request) {
        assertMaintainer(context);
        var room = findRoom(context, roomId);
        if (request.version() == null || request.version() != room.version) {
            throw new ApiException("ROOM_VERSION_CONFLICT", HttpStatus.CONFLICT, "Room version conflict");
        }
        assertZoneScope(context, request.zoneId());
        assertHierarchy(context, request.zoneId(), request.buildingId(), request.floorId());
        rooms.findByTenantIdAndFacilityIdAndFloorIdAndRoomNo(
                        context.tenantId(), context.facilityId(), request.floorId(), request.roomNo())
                .filter(existing -> !existing.id.equals(room.id))
                .ifPresent(existing -> {
                    throw duplicatedRoom();
                });
        if (request.capacity() < room.occupiedCount) {
            throw new ApiException("ROOM_OCCUPANCY_CONFLICT", HttpStatus.CONFLICT, "Room capacity is below occupied count");
        }

        room.zoneId = request.zoneId();
        room.buildingId = request.buildingId();
        room.floorId = request.floorId();
        room.roomNo = request.roomNo();
        room.displayName = request.displayName();
        room.capacity = request.capacity();
        room.status = request.status();

        try {
            var saved = rooms.save(room);
            audit("update", context, saved.id, "room updated");
            return get(context, saved.id);
        } catch (DataIntegrityViolationException exception) {
            throw duplicatedRoom();
        }
    }

    @Transactional
    public RoomDtos.RoomResponse disable(RequestContext context, String roomId) {
        assertMaintainer(context);
        var room = findRoom(context, roomId);
        if (room.occupiedCount > 0) {
            throw new ApiException("ROOM_OCCUPANCY_CONFLICT", HttpStatus.CONFLICT, "Occupied room cannot be disabled");
        }
        room.status = RoomStatus.inactive;
        var saved = rooms.save(room);
        audit("disable", context, saved.id, "room disabled");
        return get(context, saved.id);
    }

    @Transactional
    public RoomDtos.BedAssignmentResponse assignBed(
            RequestContext context, String roomId, RoomDtos.AssignBedRequest request) {
        assertAdmissionMaintainer(context);
        var room = rooms.findLockedByIdAndTenantIdAndFacilityId(roomId, context.tenantId(), context.facilityId())
                .orElseThrow(() -> new ApiException("ROOM_NOT_FOUND", HttpStatus.NOT_FOUND, "Room not found"));
        if (request.roomVersion() != null && request.roomVersion() != room.version) {
            throw new ApiException("ROOM_VERSION_CONFLICT", HttpStatus.CONFLICT, "Room version conflict");
        }
        if (room.status != RoomStatus.available || room.occupiedCount >= room.capacity) {
            throw new ApiException("ROOM_OCCUPANCY_CONFLICT", HttpStatus.CONFLICT, "Room has no available bed");
        }

        var resident = residents.findByIdAndTenantIdAndFacilityId(
                        request.residentId(), context.tenantId(), context.facilityId())
                .orElseThrow(() -> new ApiException("RESIDENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Resident not found"));
        if (bedAssignments.existsByTenantIdAndFacilityIdAndResidentIdAndStatus(
                context.tenantId(), context.facilityId(), request.residentId(), "active")) {
            throw new ApiException("BED_ASSIGNMENT_CONFLICT", HttpStatus.CONFLICT, "Resident already has active bed");
        }
        bedAssignments.findByTenantIdAndFacilityIdAndRoomIdAndBedLabelAndStatus(
                        context.tenantId(), context.facilityId(), roomId, request.bedLabel(), "active")
                .ifPresent(existing -> {
                    throw new ApiException("BED_ASSIGNMENT_CONFLICT", HttpStatus.CONFLICT, "Bed is already occupied");
                });

        var zone = findZone(activeZones(context), room.zoneId);
        var building = findBuilding(
                buildings.findByTenantIdAndFacilityId(context.tenantId(), context.facilityId()), room.buildingId);
        var floor = findFloor(floors.findByTenantIdAndFacilityId(context.tenantId(), context.facilityId()), room.floorId);
        var locationLabel = toResponse(zone, building, floor, room).locationLabel();

        var assignment = new BedAssignmentEntity();
        assignment.id = "bed-assignment-" + UUID.randomUUID();
        assignment.tenantId = context.tenantId();
        assignment.facilityId = context.facilityId();
        assignment.residentId = request.residentId();
        assignment.roomId = room.id;
        assignment.bedLabel = request.bedLabel();
        assignment.status = "active";
        assignment.startAt = Instant.now();
        bedAssignments.save(assignment);

        room.occupiedCount += 1;
        if (room.occupiedCount >= room.capacity) {
            room.status = RoomStatus.occupied;
        }
        var savedRoom = rooms.save(room);

        resident.roomId = savedRoom.id;
        resident.bedLabel = request.bedLabel();
        resident.livingLocationLabel = locationLabel;
        residents.save(resident);

        audit("assign-bed", context, savedRoom.id, "residentId=" + resident.id + ",bedLabel=" + request.bedLabel());
        return new RoomDtos.BedAssignmentResponse(
                assignment.id,
                resident.id,
                savedRoom.id,
                request.bedLabel(),
                locationLabel,
                savedRoom.occupiedCount,
                savedRoom.status,
                savedRoom.version);
    }

    @Transactional(readOnly = true)
    public List<RoomDtos.RoomResponse> available(RequestContext context) {
        return list(context, null, null, null, null).stream()
                .filter(room -> room.status() == RoomStatus.available)
                .filter(room -> room.occupiedCount() < room.capacity())
                .toList();
    }

    private List<HousingZoneEntity> activeZones(RequestContext context) {
        var zoneList = zones.findByTenantIdAndFacilityIdAndStatus(context.tenantId(), context.facilityId(), "active");
        if (zoneList.size() != 1 || !onlyActiveZoneName.equals(zoneList.getFirst().name)) {
            throw new ApiException("ROOM_ZONE_SCOPE_INVALID", HttpStatus.BAD_REQUEST, "Current release only supports 和成养老");
        }
        return zoneList;
    }

    private void assertZoneScope(RequestContext context, String zoneId) {
        var zone = findZone(activeZones(context), zoneId);
        if (!onlyActiveZoneName.equals(zone.name)) {
            throw new ApiException("ROOM_ZONE_SCOPE_INVALID", HttpStatus.BAD_REQUEST, "Current release only supports 和成养老");
        }
    }

    private void assertHierarchy(RequestContext context, String zoneId, String buildingId, String floorId) {
        var building = buildings.findById(buildingId)
                .filter(item -> item.tenantId.equals(context.tenantId()))
                .filter(item -> item.facilityId.equals(context.facilityId()))
                .orElseThrow(() -> hierarchyMismatch());
        var floor = floors.findById(floorId)
                .filter(item -> item.tenantId.equals(context.tenantId()))
                .filter(item -> item.facilityId.equals(context.facilityId()))
                .orElseThrow(() -> hierarchyMismatch());
        if (!building.zoneId.equals(zoneId) || !floor.zoneId.equals(zoneId) || !floor.buildingId.equals(buildingId)) {
            throw hierarchyMismatch();
        }
    }

    private RoomEntity findRoom(RequestContext context, String roomId) {
        return rooms.findByIdAndTenantIdAndFacilityId(roomId, context.tenantId(), context.facilityId())
                .orElseThrow(() -> new ApiException("ROOM_NOT_FOUND", HttpStatus.NOT_FOUND, "Room not found"));
    }

    private HousingZoneEntity findZone(List<HousingZoneEntity> zoneList, String id) {
        return zoneList.stream().filter(zone -> zone.id.equals(id)).findFirst().orElseThrow(this::hierarchyMismatch);
    }

    private BuildingEntity findBuilding(List<BuildingEntity> buildingList, String id) {
        return buildingList.stream().filter(building -> building.id.equals(id)).findFirst().orElseThrow(this::hierarchyMismatch);
    }

    private FloorEntity findFloor(List<FloorEntity> floorList, String id) {
        return floorList.stream().filter(floor -> floor.id.equals(id)).findFirst().orElseThrow(this::hierarchyMismatch);
    }

    private RoomDtos.RoomResponse toResponse(
            HousingZoneEntity zone, BuildingEntity building, FloorEntity floor, RoomEntity room) {
        return new RoomDtos.RoomResponse(
                room.id,
                room.zoneId,
                room.buildingId,
                room.floorId,
                room.roomNo,
                room.displayName,
                zone.name + " - " + building.name + " - " + floor.name + " - " + room.displayName,
                room.capacity,
                room.occupiedCount,
                room.status,
                room.version);
    }

    private ApiException hierarchyMismatch() {
        return new ApiException("ROOM_HIERARCHY_MISMATCH", HttpStatus.BAD_REQUEST, "Room hierarchy mismatch");
    }

    private ApiException duplicatedRoom() {
        return new ApiException("ROOM_DUPLICATED", HttpStatus.CONFLICT, "Room number already exists in this floor");
    }

    private void assertMaintainer(RequestContext context) {
        if (!List.of("property-manager", "property-supervisor", "department-manager", "admin").contains(context.role())) {
            throw new ApiException("ROOM_FORBIDDEN", HttpStatus.FORBIDDEN, "Current role cannot maintain rooms");
        }
    }

    private void assertAdmissionMaintainer(RequestContext context) {
        if (!List.of("social-worker", "social-worker-supervisor", "department-manager", "admin").contains(context.role())) {
            throw new ApiException("ROOM_FORBIDDEN", HttpStatus.FORBIDDEN, "Current role cannot assign resident beds");
        }
    }

    private void audit(String action, RequestContext context, String roomId, String detail) {
        auditService.record(context, "room-management", action, "room", roomId, detail);
        log.info("room_audit action={} roomId={} tenantId={} facilityId={} role={}",
                action, roomId, context.tenantId(), context.facilityId(), context.role());
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

    private String hashRoomRequest(RoomDtos.UpsertRoomRequest request) {
        return sha256(String.join("|",
                request.zoneId(),
                request.buildingId(),
                request.floorId(),
                request.roomNo(),
                request.displayName(),
                Integer.toString(request.capacity()),
                request.status().name()));
    }

    private String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
