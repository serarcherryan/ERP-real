package com.erpreal.backend.room;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "buildings")
public class BuildingEntity {
    @Id
    public String id;
    public String tenantId;
    public String facilityId;
    public String zoneId;
    public String name;
    public int sortOrder;
    public String status;
    @Version
    public long version;
}
