package com.erpreal.backend.idempotency;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, String> {
    Optional<IdempotencyRecordEntity> findByTenantIdAndFacilityIdAndActionAndIdempotencyKey(
            String tenantId, String facilityId, String action, String idempotencyKey);
}
