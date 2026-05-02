package com.erpreal.backend.resident;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyContactRepository extends JpaRepository<FamilyContactEntity, String> {
    List<FamilyContactEntity> findByTenantIdAndFacilityIdAndResidentIdAndStatusOrderByPriorityAsc(
            String tenantId, String facilityId, String residentId, String status);

    void deleteByTenantIdAndFacilityIdAndResidentId(String tenantId, String facilityId, String residentId);
}
