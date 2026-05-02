package com.erpreal.backend.room;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "rooms")
public class RoomEntity {
    @Id
    public String id;
    public String tenantId;
    public String facilityId;
    public String zoneId;
    public String buildingId;
    public String floorId;
    public String roomNo;
    public String displayName;
    public int capacity;
    public int occupiedCount;
    @Enumerated(EnumType.STRING)
    public RoomStatus status;
    @Version
    public long version;
}
