package com.erpreal.backend.auth;

public record AuthenticatedUser(
        String userId,
        String displayName,
        String tenantId,
        String facilityId,
        String role,
        long permissionVersion,
        boolean superAdmin) {
}
