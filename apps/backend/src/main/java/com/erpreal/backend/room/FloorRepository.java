package com.erpreal.backend.room;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<FloorEntity, String> {
    List<FloorEntity> findByTenantIdAndFacilityId(String tenantId, String facilityId);
}
