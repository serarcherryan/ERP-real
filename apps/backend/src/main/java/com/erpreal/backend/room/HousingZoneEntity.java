package com.erpreal.backend.room;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "housing_zones")
public class HousingZoneEntity {
    @Id
    public String id;
    public String tenantId;
    public String facilityId;
    public String name;
    public String status;
    @Version
    public long version;
}
