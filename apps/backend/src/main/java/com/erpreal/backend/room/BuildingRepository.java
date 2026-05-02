package com.erpreal.backend.room;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<BuildingEntity, String> {
    List<BuildingEntity> findByTenantIdAndFacilityId(String tenantId, String facilityId);
}
