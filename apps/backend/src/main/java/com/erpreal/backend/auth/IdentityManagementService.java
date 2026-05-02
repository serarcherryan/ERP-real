package com.erpreal.backend.auth;

import com.erpreal.backend.audit.AuditService;
import com.erpreal.backend.common.ApiException;
import com.erpreal.backend.room.RequestContext;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityManagementService {
    private final SysUserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final UserRoleRepository userRoles;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public IdentityManagementService(SysUserRepository users,
            RoleRepository roles,
            PermissionRepository permissions,
            UserRoleRepository userRoles,
            PermissionService permissionService,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.users = users;
        this.roles = roles;
        this.permissions = permissions;
        this.userRoles = userRoles;
        this.permissionService = permissionService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public List<IdentityDtos.PermissionResponse> permissions(RequestContext context) {
        permissionService.require(context, PermissionCodes.IDENTITY_ROLE_READ, "IDENTITY_FORBIDDEN",
                "Current user cannot read permissions");
        return permissions.findAllByOrderByCodeAsc().stream()
                .map(this::toPermission)
                .toList();
    }

    public List<IdentityDtos.RoleResponse> roles(RequestContext context) {
        permissionService.require(context, PermissionCodes.IDENTITY_ROLE_READ, "IDENTITY_FORBIDDEN",
                "Current user cannot read roles");
        return permissionService.roleSummaries(context.tenantId()).stream()
                .map(this::toRole)
                .toList();
    }

    public IdentityDtos.RoleResponse role(RequestContext context, String roleId) {
        permissionService.require(context, PermissionCodes.IDENTITY_ROLE_READ, "IDENTITY_FORBIDDEN",
                "Current user cannot read roles");
        return toRole(permissionService.roleSummary(context.tenantId(), roleId));
    }

    @Transactional
    public IdentityDtos.RoleResponse replaceRolePermissions(
            RequestContext context,
            String roleId,
            IdentityDtos.ReplaceRolePermissionsRequest request) {
        permissionService.require(context, PermissionCodes.IDENTITY_ROLE_UPDATE_PERMISSIONS, "IDENTITY_FORBIDDEN",
                "Current user cannot update role permissions");
        var summary = permissionService.replaceRolePermissions(context, roleId, request.permissionCodes());
        var codes = request.permissionCodes() == null ? List.<String>of() : request.permissionCodes();
        auditService.record(context, "auth-permission", "UPDATE_ROLE_PERMISSIONS", "role", roleId,
                "permissionCodes=" + String.join(",", codes));
        return toRole(summary);
    }

    public List<IdentityDtos.UserResponse> users(RequestContext context) {
        permissionService.require(context, PermissionCodes.IDENTITY_USER_READ, "IDENTITY_FORBIDDEN",
                "Current user cannot read users");
        return users.findByTenantIdAndFacilityIdOrderByUsernameAsc(context.tenantId(), context.facilityId()).stream()
                .map(this::toUser)
                .toList();
    }

    public IdentityDtos.UserResponse user(RequestContext context, String userId) {
        permissionService.require(context, PermissionCodes.IDENTITY_USER_READ, "IDENTITY_FORBIDDEN",
                "Current user cannot read users");
        return toUser(findUser(context, userId));
    }

    @Transactional
    public IdentityDtos.UserResponse createUser(RequestContext context, IdentityDtos.CreateUserRequest request) {
        permissionService.require(context, PermissionCodes.IDENTITY_USER_CREATE, "IDENTITY_FORBIDDEN",
                "Current user cannot create users");
        var username = request.username().trim();
        if (users.findByTenantIdAndUsername(context.tenantId(), username).isPresent()) {
            throw new ApiException("IDENTITY_USER_DUPLICATED", HttpStatus.CONFLICT, "Username already exists");
        }
        var roleList = validateRoles(context, request.roleIds());
        var user = new SysUserEntity();
        user.setId("user-" + UUID.randomUUID());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setRole(roleList.get(0).code);
        user.setTenantId(context.tenantId());
        user.setFacilityId(context.facilityId());
        user.setEnabled(true);
        user.setSuperAdmin(false);
        user.setPermissionVersion(1);
        users.save(user);
        replaceBindings(context.tenantId(), user.getId(), roleList);
        auditService.record(context, "auth-permission", "CREATE_USER", "sys_user", user.getId(),
                "username=" + username);
        return toUser(user);
    }

    @Transactional
    public IdentityDtos.UserResponse replaceUserRoles(
            RequestContext context,
            String userId,
            IdentityDtos.ReplaceUserRolesRequest request) {
        permissionService.require(context, PermissionCodes.IDENTITY_USER_UPDATE, "IDENTITY_FORBIDDEN",
                "Current user cannot update users");
        if (context.userId().equals(userId)) {
            throw new ApiException("IDENTITY_SELF_ROLE_CHANGE", HttpStatus.FORBIDDEN,
                    "Cannot modify your own roles");
        }
        var user = findUser(context, userId);
        var roleList = validateRoles(context, request.roleIds());
        user.setRole(roleList.get(0).code);
        user.setPermissionVersion(user.getPermissionVersion() + 1);
        user.setUpdatedAt(Instant.now());
        users.save(user);
        replaceBindings(context.tenantId(), user.getId(), roleList);
        auditService.record(context, "auth-permission", "UPDATE_USER_ROLES", "sys_user", user.getId(),
                "roleIds=" + String.join(",", request.roleIds()));
        return toUser(user);
    }

    @Transactional
    public IdentityDtos.UserResponse disableUser(RequestContext context, String userId) {
        permissionService.require(context, PermissionCodes.IDENTITY_USER_DISABLE, "IDENTITY_FORBIDDEN",
                "Current user cannot disable users");
        if (context.userId().equals(userId)) {
            throw new ApiException("IDENTITY_SELF_DISABLE", HttpStatus.FORBIDDEN,
                    "Cannot disable yourself");
        }
        var user = findUser(context, userId);
        if (user.isSuperAdmin()) {
            throw new ApiException("IDENTITY_SUPER_ADMIN_PROTECTED", HttpStatus.FORBIDDEN,
                    "Cannot disable a super admin account");
        }
        user.setEnabled(false);
        user.setPermissionVersion(user.getPermissionVersion() + 1);
        user.setUpdatedAt(Instant.now());
        users.save(user);
        auditService.record(context, "auth-permission", "DISABLE_USER", "sys_user", user.getId(), "user disabled");
        return toUser(user);
    }

    private SysUserEntity findUser(RequestContext context, String userId) {
        return users.findByTenantIdAndId(context.tenantId(), userId)
                .filter(user -> context.facilityId().equals(user.getFacilityId()))
                .orElseThrow(() -> new ApiException("IDENTITY_USER_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "User not found"));
    }

    private List<RoleEntity> validateRoles(RequestContext context, List<String> roleIds) {
        var uniqueRoleIds = new LinkedHashSet<>(roleIds);
        var roleList = uniqueRoleIds.stream()
                .map(roleId -> roles.findByTenantIdAndIdAndEnabledTrue(context.tenantId(), roleId)
                        .orElseThrow(() -> new ApiException("IDENTITY_ROLE_NOT_FOUND", HttpStatus.NOT_FOUND,
                                "Role not found")))
                .toList();
        if (roleList.isEmpty()) {
            throw new ApiException("IDENTITY_ROLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Role not found");
        }
        return roleList;
    }

    private void replaceBindings(String tenantId, String userId, List<RoleEntity> roleList) {
        userRoles.deleteByTenantIdAndUserId(tenantId, userId);
        userRoles.flush();
        var now = Instant.now();
        var bindings = roleList.stream()
                .map(role -> {
                    var entity = new UserRoleEntity();
                    entity.id = "ur-" + UUID.randomUUID();
                    entity.tenantId = tenantId;
                    entity.userId = userId;
                    entity.roleId = role.id;
                    entity.createdAt = now;
                    return entity;
                })
                .toList();
        userRoles.saveAll(bindings);
    }

    private IdentityDtos.PermissionResponse toPermission(PermissionEntity permission) {
        return new IdentityDtos.PermissionResponse(
                permission.id,
                permission.code,
                permission.name,
                permission.moduleName,
                permission.action,
                permission.riskLevel);
    }

    private IdentityDtos.RoleResponse toRole(PermissionService.RolePermissionSummary summary) {
        var role = summary.role();
        return new IdentityDtos.RoleResponse(
                role.id,
                role.code,
                role.name,
                role.description,
                role.systemBuiltin,
                role.enabled,
                summary.permissionCodes());
    }

    private IdentityDtos.UserResponse toUser(SysUserEntity user) {
        var snapshot = permissionService.snapshot(user.toAuthenticatedUser());
        return new IdentityDtos.UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getTenantId(),
                user.getFacilityId(),
                user.isEnabled(),
                snapshot.superAdmin(),
                snapshot.permissionVersion(),
                snapshot.roles(),
                snapshot.permissions());
    }
}
