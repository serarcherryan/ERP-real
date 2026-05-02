package com.erpreal.backend.auth;

import com.erpreal.backend.common.ApiResponse;
import com.erpreal.backend.room.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class IdentityManagementController {
    private final IdentityManagementService service;

    public IdentityManagementController(IdentityManagementService service) {
        this.service = service;
    }

    @GetMapping("/permissions")
    ApiResponse<List<IdentityDtos.PermissionResponse>> permissions(HttpServletRequest request) {
        return response(service.permissions(RequestContext.from(request)), request);
    }

    @GetMapping("/roles")
    ApiResponse<List<IdentityDtos.RoleResponse>> roles(HttpServletRequest request) {
        return response(service.roles(RequestContext.from(request)), request);
    }

    @GetMapping("/roles/{roleId}")
    ApiResponse<IdentityDtos.RoleResponse> role(HttpServletRequest request, @PathVariable String roleId) {
        return response(service.role(RequestContext.from(request), roleId), request);
    }

    @PutMapping("/roles/{roleId}/permissions")
    ApiResponse<IdentityDtos.RoleResponse> replaceRolePermissions(
            HttpServletRequest request,
            @PathVariable String roleId,
            @Valid @RequestBody IdentityDtos.ReplaceRolePermissionsRequest body) {
        return response(service.replaceRolePermissions(RequestContext.from(request), roleId, body), request);
    }

    @GetMapping("/users")
    ApiResponse<List<IdentityDtos.UserResponse>> users(HttpServletRequest request) {
        return response(service.users(RequestContext.from(request)), request);
    }

    @PostMapping("/users")
    ApiResponse<IdentityDtos.UserResponse> createUser(
            HttpServletRequest request,
            @Valid @RequestBody IdentityDtos.CreateUserRequest body) {
        return response(service.createUser(RequestContext.from(request), body), request);
    }

    @GetMapping("/users/{userId}")
    ApiResponse<IdentityDtos.UserResponse> user(HttpServletRequest request, @PathVariable String userId) {
        return response(service.user(RequestContext.from(request), userId), request);
    }

    @PutMapping("/users/{userId}/roles")
    ApiResponse<IdentityDtos.UserResponse> replaceUserRoles(
            HttpServletRequest request,
            @PathVariable String userId,
            @Valid @RequestBody IdentityDtos.ReplaceUserRolesRequest body) {
        return response(service.replaceUserRoles(RequestContext.from(request), userId, body), request);
    }

    @PostMapping("/users/{userId}/disable")
    ApiResponse<IdentityDtos.UserResponse> disableUser(HttpServletRequest request, @PathVariable String userId) {
        return response(service.disableUser(RequestContext.from(request), userId), request);
    }

    private <T> ApiResponse<T> response(T data, HttpServletRequest request) {
        var traceId = request.getHeader("X-Trace-Id");
        return new ApiResponse<>(data, traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId);
    }
}
