package com.erpreal.backend.room;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<RoomEntity, String> {
    List<RoomEntity> findByTenantIdAndFacilityId(String tenantId, String facilityId);

    Optional<RoomEntity> findByIdAndTenantIdAndFacilityId(String id, String tenantId, String facilityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select room from RoomEntity room
            where room.id = :id and room.tenantId = :tenantId and room.facilityId = :facilityId
            """)
    Optional<RoomEntity> findLockedByIdAndTenantIdAndFacilityId(
            @Param("id") String id,
            @Param("tenantId") String tenantId,
            @Param("facilityId") String facilityId);

    Optional<RoomEntity> findByTenantIdAndFacilityIdAndFloorIdAndRoomNo(
            String tenantId, String facilityId, String floorId, String roomNo);

    boolean existsByTenantIdAndFacilityIdAndFloorIdAndRoomNo(
            String tenantId, String facilityId, String floorId, String roomNo);
}
