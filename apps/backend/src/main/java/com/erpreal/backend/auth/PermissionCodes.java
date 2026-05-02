package com.erpreal.backend.auth;

public final class PermissionCodes {
    private PermissionCodes() {
    }

    public static final String RESIDENT_PROFILE_CREATE = "resident.profile:create";
    public static final String RESIDENT_PROFILE_READ = "resident.profile:read";
    public static final String RESIDENT_PROFILE_UPDATE = "resident.profile:update";
    public static final String RESIDENT_PROFILE_DELETE = "resident.profile:delete";
    public static final String IDENTITY_USER_CREATE = "identity.user:create";
    public static final String IDENTITY_USER_READ = "identity.user:read";
    public static final String IDENTITY_USER_UPDATE = "identity.user:update";
    public static final String IDENTITY_USER_DISABLE = "identity.user:disable";
    public static final String IDENTITY_ROLE_READ = "identity.role:read";
    public static final String IDENTITY_ROLE_UPDATE_PERMISSIONS = "identity.role:update_permissions";
}
