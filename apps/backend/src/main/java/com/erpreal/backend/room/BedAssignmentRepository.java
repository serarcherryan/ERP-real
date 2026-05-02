package com.erpreal.backend.room;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BedAssignmentRepository extends JpaRepository<BedAssignmentEntity, String> {
    boolean existsByTenantIdAndFacilityIdAndResidentIdAndStatus(
            String tenantId, String facilityId, String residentId, String status);

    Optional<BedAssignmentEntity> findByTenantIdAndFacilityIdAndRoomIdAndBedLabelAndStatus(
            String tenantId, String facilityId, String roomId, String bedLabel, String status);
}
