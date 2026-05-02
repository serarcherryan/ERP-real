package com.erpreal.backend.resident;

import com.erpreal.backend.common.ApiException;
import com.erpreal.backend.common.ApiResponse;
import com.erpreal.backend.room.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/residents")
public class ResidentProfileController {
    private final ResidentProfileService service;

    public ResidentProfileController(ResidentProfileService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<ResidentDtos.ResidentPageResponse> list(
            HttpServletRequest request,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return response(service.list(RequestContext.from(request), keyword, status, page, pageSize), request);
    }

    @PostMapping
    ApiResponse<ResidentDtos.ResidentDetailResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody ResidentDtos.UpsertResidentRequest body) {
        return response(service.create(RequestContext.from(request), body), request);
    }

    @GetMapping("/{residentId}")
    ApiResponse<ResidentDtos.ResidentDetailResponse> get(HttpServletRequest request, @PathVariable String residentId) {
        return response(service.get(RequestContext.from(request), residentId), request);
    }

    @PatchMapping("/{residentId}")
    ApiResponse<ResidentDtos.ResidentDetailResponse> update(
            HttpServletRequest request,
            @PathVariable String residentId,
            @Valid @RequestBody ResidentDtos.UpsertResidentRequest body) {
        return response(service.update(RequestContext.from(request), residentId, body), request);
    }

    @PostMapping("/{residentId}/void")
    ApiResponse<ResidentDtos.ResidentDetailResponse> voidResident(
            HttpServletRequest request,
            @PathVariable String residentId,
            @Valid @RequestBody ResidentDtos.VoidResidentRequest body) {
        return response(service.voidResident(RequestContext.from(request), residentId, body), request);
    }

    @PostMapping("/export-tasks")
    ApiResponse<Void> exportTasks() {
        throw new ApiException("RESIDENT_PROFILE_EXPORT_NOT_IMPLEMENTED", HttpStatus.NOT_IMPLEMENTED,
                "Resident profile export task is not implemented yet");
    }

    private <T> ApiResponse<T> response(T data, HttpServletRequest request) {
        var traceId = request.getHeader("X-Trace-Id");
        return new ApiResponse<>(data, traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId);
    }
}
