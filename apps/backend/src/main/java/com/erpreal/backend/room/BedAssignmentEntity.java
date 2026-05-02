package com.erpreal.backend.room;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "bed_assignments")
public class BedAssignmentEntity {
    @Id
    public String id;
    public String tenantId;
    public String facilityId;
    public String residentId;
    public String roomId;
    public String bedLabel;
    public String status;
    public Instant startAt;
    public Instant endAt;
    @Version
    public long version;
}
