package com.erpreal.backend.room;

import com.erpreal.backend.auth.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public record RequestContext(
        String tenantId,
        String facilityId,
        String userId,
        String displayName,
        String role,
        String traceId,
        String idempotencyKey) {
    public static RequestContext from(HttpServletRequest request) {
        var user = AuthContext.requireUser(request);
        return new RequestContext(
                user.tenantId(),
                user.facilityId(),
                user.userId(),
                user.displayName(),
                user.role(),
                headerOrDefault(request, "X-Trace-Id", UUID.randomUUID().toString()),
                blankToNull(request.getHeader("Idempotency-Key")));
    }

    private static String headerOrDefault(HttpServletRequest request, String name, String fallback) {
        var value = request.getHeader(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
