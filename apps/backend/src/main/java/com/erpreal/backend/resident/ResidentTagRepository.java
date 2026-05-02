package com.erpreal.backend.resident;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentTagRepository extends JpaRepository<ResidentTagEntity, String> {
    List<ResidentTagEntity> findByTenantIdAndFacilityIdAndResidentIdAndStatusOrderByTagNameAsc(
            String tenantId, String facilityId, String residentId, String status);

    void deleteByTenantIdAndFacilityIdAndResidentId(String tenantId, String facilityId, String residentId);
}
