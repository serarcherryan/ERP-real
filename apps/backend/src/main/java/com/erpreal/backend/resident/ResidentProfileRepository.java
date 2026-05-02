package com.erpreal.backend.resident;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResidentProfileRepository extends JpaRepository<ResidentProfileEntity, String> {
    Optional<ResidentProfileEntity> findByIdAndTenantIdAndFacilityId(String id, String tenantId, String facilityId);

    Optional<ResidentProfileEntity> findByTenantIdAndResidentNo(String tenantId, String residentNo);

    Optional<ResidentProfileEntity> findByTenantIdAndIdentityNoHash(String tenantId, String identityNoHash);

    @Query("""
            select resident from ResidentProfileEntity resident
            where resident.tenantId = :tenantId
              and resident.facilityId = :facilityId
              and (:status is null or resident.status = :status)
            """)
    Page<ResidentProfileEntity> searchWithoutKeyword(
            @Param("tenantId") String tenantId,
            @Param("facilityId") String facilityId,
            @Param("status") String status,
            Pageable pageable);

    @Query("""
            select resident from ResidentProfileEntity resident
            where resident.tenantId = :tenantId
              and resident.facilityId = :facilityId
              and (:status is null or resident.status = :status)
              and (lower(resident.name) like lower(concat('%', :keyword, '%'))
                   or lower(resident.residentNo) like lower(concat('%', :keyword, '%'))
                   or (resident.roomLabel is not null and lower(resident.roomLabel) like lower(concat('%', :keyword, '%')))
                   or (resident.livingLocationLabel is not null and lower(resident.livingLocationLabel) like lower(concat('%', :keyword, '%'))))
            """)
    Page<ResidentProfileEntity> search(
            @Param("tenantId") String tenantId,
            @Param("facilityId") String facilityId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
