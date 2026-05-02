package com.erpreal.backend.resident;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentHealthSummaryRepository extends JpaRepository<ResidentHealthSummaryEntity, String> {
    Optional<ResidentHealthSummaryEntity> findByTenantIdAndFacilityIdAndResidentId(
            String tenantId, String facilityId, String residentId);

    void deleteByTenantIdAndFacilityIdAndResidentId(String tenantId, String facilityId, String residentId);
}
