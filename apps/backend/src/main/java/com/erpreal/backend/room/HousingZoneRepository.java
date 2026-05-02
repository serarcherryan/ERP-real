package com.erpreal.backend.room;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HousingZoneRepository extends JpaRepository<HousingZoneEntity, String> {
    List<HousingZoneEntity> findByTenantIdAndFacilityIdAndStatus(String tenantId, String facilityId, String status);
}
