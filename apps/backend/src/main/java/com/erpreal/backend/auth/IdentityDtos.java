package com.erpreal.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public final class IdentityDtos {
    private IdentityDtos() {
    }

    public record PermissionResponse(
            String id,
            String code,
            String name,
            String moduleName,
            String action,
            String riskLevel) {
    }

    public record RoleResponse(
            String id,
            String code,
            String name,
            String description,
            boolean systemBuiltin,
            boolean enabled,
            Set<String> permissionCodes) {
    }

    public record UserResponse(
            String id,
            String username,
            String displayName,
            String role,
            String tenantId,
            String facilityId,
            boolean enabled,
            boolean superAdmin,
            long permissionVersion,
            Set<String> roles,
            Set<String> permissions) {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank @Size(max = 120) String displayName,
            @NotEmpty List<@NotBlank String> roleIds) {
    }

    public record ReplaceUserRolesRequest(@NotEmpty List<@NotBlank String> roleIds) {
    }

    public record ReplaceRolePermissionsRequest(@NotEmpty List<@NotBlank String> permissionCodes) {
    }
}
