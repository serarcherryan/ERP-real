package com.erpreal.backend.auth;

import com.erpreal.backend.common.ApiException;
import com.erpreal.backend.room.RequestContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {
    private final SysUserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final UserRoleRepository userRoles;
    private final RolePermissionRepository rolePermissions;

    public PermissionService(SysUserRepository users,
            RoleRepository roles,
            PermissionRepository permissions,
            UserRoleRepository userRoles,
            RolePermissionRepository rolePermissions) {
        this.users = users;
        this.roles = roles;
        this.permissions = permissions;
        this.userRoles = userRoles;
        this.rolePermissions = rolePermissions;
    }

    public void require(RequestContext context, String permissionCode, String errorCode, String message) {
        if (!hasPermission(context, permissionCode)) {
            throw new ApiException(errorCode, HttpStatus.FORBIDDEN, message);
        }
    }

    public boolean hasPermission(RequestContext context, String permissionCode) {
        return permissionCodes(context).contains(permissionCode);
    }

    public UserPermissionSnapshot snapshot(AuthenticatedUser authenticatedUser) {
        var sysUser = users.findByTenantIdAndId(authenticatedUser.tenantId(), authenticatedUser.userId()).orElse(null);
        var superAdmin = sysUser != null ? sysUser.isSuperAdmin() : authenticatedUser.superAdmin();
        var permissionVersion = sysUser != null ? sysUser.getPermissionVersion() : authenticatedUser.permissionVersion();
        var roleCodes = roleCodes(authenticatedUser.tenantId(), authenticatedUser.userId(), authenticatedUser.role());
        var codes = superAdmin
                ? allPermissionCodes()
                : permissionCodesForRoles(authenticatedUser.tenantId(), roleCodes, authenticatedUser.role());
        return new UserPermissionSnapshot(roleCodes, codes, permissionVersion, superAdmin);
    }

    public Set<String> permissionCodes(RequestContext context) {
        var user = users.findByTenantIdAndId(context.tenantId(), context.userId()).orElse(null);
        if (user != null && user.isSuperAdmin()) {
            return allPermissionCodes();
        }
        var roleCodes = roleCodes(context.tenantId(), context.userId(), context.role());
        return permissionCodesForRoles(context.tenantId(), roleCodes, context.role());
    }

    public Set<String> allPermissionCodes() {
        return permissions.findAllByOrderByCodeAsc().stream()
                .map(permission -> permission.code)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<RolePermissionSummary> roleSummaries(String tenantId) {
        return roles.findByTenantIdAndEnabledTrueOrderByCodeAsc(tenantId).stream()
                .map(role -> new RolePermissionSummary(role, permissionCodesForRole(tenantId, role.id)))
                .toList();
    }

    public RolePermissionSummary roleSummary(String tenantId, String roleId) {
        var role = roles.findByTenantIdAndIdAndEnabledTrue(tenantId, roleId)
                .orElseThrow(() -> new ApiException("IDENTITY_ROLE_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Role not found"));
        return new RolePermissionSummary(role, permissionCodesForRole(tenantId, role.id));
    }

    @Transactional
    public RolePermissionSummary replaceRolePermissions(RequestContext context, String roleId, List<String> permissionCodes) {
        var role = roles.findByTenantIdAndIdAndEnabledTrue(context.tenantId(), roleId)
                .orElseThrow(() -> new ApiException("IDENTITY_ROLE_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Role not found"));
        var requested = new LinkedHashSet<>(permissionCodes == null ? List.of() : permissionCodes);
        var permissionByCode = permissions.findByCodeIn(requested).stream()
                .collect(Collectors.toMap(permission -> permission.code, Function.identity()));
        if (permissionByCode.size() != requested.size()) {
            throw new ApiException("IDENTITY_PERMISSION_NOT_FOUND", HttpStatus.BAD_REQUEST,
                    "Permission code not found");
        }

        rolePermissions.deleteByTenantIdAndRoleId(context.tenantId(), role.id);
        rolePermissions.flush();
        var now = Instant.now();
        var nextBindings = requested.stream()
                .sorted()
                .map(code -> {
                    var entity = new RolePermissionEntity();
                    entity.id = "rp-" + UUID.randomUUID();
                    entity.tenantId = context.tenantId();
                    entity.roleId = role.id;
                    entity.permissionId = permissionByCode.get(code).id;
                    entity.createdBy = context.userId();
                    entity.createdAt = now;
                    return entity;
                })
                .toList();
        rolePermissions.saveAll(nextBindings);
        bumpUsersForRole(context.tenantId(), role.id);
        return new RolePermissionSummary(role, requested);
    }

    private void bumpUsersForRole(String tenantId, String roleId) {
        for (var userRole : userRoles.findByTenantIdAndRoleId(tenantId, roleId)) {
            users.findByTenantIdAndId(tenantId, userRole.userId).ifPresent(user -> {
                user.setPermissionVersion(user.getPermissionVersion() + 1);
                user.setUpdatedAt(Instant.now());
                users.save(user);
            });
        }
    }

    private Set<String> roleCodes(String tenantId, String userId, String fallbackRoleCode) {
        var bindings = userRoles.findByTenantIdAndUserId(tenantId, userId);
        if (bindings.isEmpty()) {
            return Set.of(fallbackRoleCode);
        }
        var roleById = roles.findByTenantIdAndEnabledTrueOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(role -> role.id, Function.identity()));
        var codes = bindings.stream()
                .map(binding -> roleById.get(binding.roleId))
                .filter(role -> role != null && role.enabled)
                .map(role -> role.code)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return codes.isEmpty() ? Set.of(fallbackRoleCode) : codes;
    }

    private Set<String> permissionCodesForRoles(String tenantId, Set<String> roleCodes, String fallbackRoleCode) {
        var allRoles = roles.findByTenantIdAndEnabledTrueOrderByCodeAsc(tenantId);
        var roleIds = allRoles.stream()
                .filter(role -> roleCodes.contains(role.code))
                .map(role -> role.id)
                .toList();
        if (roleIds.isEmpty() && fallbackRoleCode != null) {
            return fallbackPermissions(fallbackRoleCode);
        }
        var permissionById = permissions.findAll().stream()
                .collect(Collectors.toMap(permission -> permission.id, permission -> permission.code));
        return rolePermissions.findByTenantIdAndRoleIdIn(tenantId, roleIds).stream()
                .map(binding -> permissionById.get(binding.permissionId))
                .filter(code -> code != null)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> permissionCodesForRole(String tenantId, String roleId) {
        var permissionById = permissions.findAll().stream()
                .collect(Collectors.toMap(permission -> permission.id, permission -> permission.code));
        return rolePermissions.findByTenantIdAndRoleId(tenantId, roleId).stream()
                .map(binding -> permissionById.get(binding.permissionId))
                .filter(code -> code != null)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> fallbackPermissions(String roleCode) {
        var codes = new ArrayList<String>();
        if ("admin".equals(roleCode)) {
            return allPermissionCodes();
        }
        if (List.of("social-worker", "social-worker-supervisor", "department-manager",
                "property-manager", "property-supervisor").contains(roleCode)) {
            codes.add(PermissionCodes.RESIDENT_PROFILE_CREATE);
            codes.add(PermissionCodes.RESIDENT_PROFILE_READ);
        }
        if (List.of("social-worker-supervisor", "department-manager",
                "property-manager", "property-supervisor").contains(roleCode)) {
            codes.add(PermissionCodes.RESIDENT_PROFILE_UPDATE);
            codes.add(PermissionCodes.RESIDENT_PROFILE_DELETE);
        }
        if (List.of("department-manager", "property-manager").contains(roleCode)) {
            codes.addAll(List.of(
                    PermissionCodes.IDENTITY_USER_CREATE,
                    PermissionCodes.IDENTITY_USER_READ,
                    PermissionCodes.IDENTITY_USER_UPDATE,
                    PermissionCodes.IDENTITY_USER_DISABLE,
                    PermissionCodes.IDENTITY_ROLE_READ,
                    PermissionCodes.IDENTITY_ROLE_UPDATE_PERMISSIONS));
        }
        return codes.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public record UserPermissionSnapshot(
            Set<String> roles,
            Set<String> permissions,
            long permissionVersion,
            boolean superAdmin) {
    }

    public record RolePermissionSummary(RoleEntity role, Set<String> permissionCodes) {
    }
}
