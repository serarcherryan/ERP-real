package com.erpreal.backend.room;

import com.erpreal.backend.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {
    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @GetMapping("/tree")
    ApiResponse<RoomDtos.RoomTreeResponse> tree(HttpServletRequest request) {
        return response(service.tree(RequestContext.from(request)), request);
    }

    @GetMapping
    ApiResponse<List<RoomDtos.RoomResponse>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String zoneId,
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) String floorId,
            @RequestParam(required = false) RoomStatus status) {
        return response(service.list(RequestContext.from(request), zoneId, buildingId, floorId, status), request);
    }

    @GetMapping("/{roomId}")
    ApiResponse<RoomDtos.RoomResponse> get(HttpServletRequest request, @PathVariable String roomId) {
        return response(service.get(RequestContext.from(request), roomId), request);
    }

    @PostMapping
    ApiResponse<RoomDtos.RoomResponse> create(
            HttpServletRequest request, @Valid @RequestBody RoomDtos.UpsertRoomRequest body) {
        return response(service.create(RequestContext.from(request), body), request);
    }

    @PatchMapping("/{roomId}")
    ApiResponse<RoomDtos.RoomResponse> update(
            HttpServletRequest request,
            @PathVariable String roomId,
            @Valid @RequestBody RoomDtos.UpsertRoomRequest body) {
        return response(service.update(RequestContext.from(request), roomId, body), request);
    }

    @PostMapping("/{roomId}/disable")
    ApiResponse<RoomDtos.RoomResponse> disable(HttpServletRequest request, @PathVariable String roomId) {
        return response(service.disable(RequestContext.from(request), roomId), request);
    }

    @PostMapping("/{roomId}/assignments")
    ApiResponse<RoomDtos.BedAssignmentResponse> assignBed(
            HttpServletRequest request,
            @PathVariable String roomId,
            @Valid @RequestBody RoomDtos.AssignBedRequest body) {
        return response(service.assignBed(RequestContext.from(request), roomId, body), request);
    }

    @GetMapping("/available")
    ApiResponse<List<RoomDtos.RoomResponse>> available(HttpServletRequest request) {
        return response(service.available(RequestContext.from(request)), request);
    }

    private <T> ApiResponse<T> response(T data, HttpServletRequest request) {
        var traceId = request.getHeader("X-Trace-Id");
        return new ApiResponse<>(data, traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId);
    }
}
