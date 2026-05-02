package com.erpreal.backend.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class RoomDtos {
    private RoomDtos() {
    }

    public record RoomTreeResponse(List<ZoneNode> zones) {
    }

    public record ZoneNode(String id, String name, List<BuildingNode> buildings) {
    }

    public record BuildingNode(String id, String name, List<FloorNode> floors) {
    }

    public record FloorNode(String id, String name, List<RoomResponse> rooms) {
    }

    public record RoomResponse(
            String id,
            String zoneId,
            String buildingId,
            String floorId,
            String roomNo,
            String displayName,
            String locationLabel,
            int capacity,
            int occupiedCount,
            RoomStatus status,
            long version) {
    }

    public record UpsertRoomRequest(
            @NotBlank String zoneId,
            @NotBlank String buildingId,
            @NotBlank String floorId,
            @NotBlank String roomNo,
            @NotBlank String displayName,
            @Min(1) @Max(8) int capacity,
            @NotNull RoomStatus status,
            Long version) {
    }

    public record AssignBedRequest(
            @NotBlank String residentId,
            @NotBlank String bedLabel,
            Long roomVersion) {
    }

    public record BedAssignmentResponse(
            String assignmentId,
            String residentId,
            String roomId,
            String bedLabel,
            String locationLabel,
            int occupiedCount,
            RoomStatus roomStatus,
            long roomVersion) {
    }
}
