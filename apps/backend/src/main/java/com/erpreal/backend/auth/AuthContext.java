package com.erpreal.backend.auth;

import com.erpreal.backend.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

public final class AuthContext {
    public static final String REQUEST_ATTRIBUTE = AuthContext.class.getName() + ".authenticatedUser";

    private AuthContext() {
    }

    public static AuthenticatedUser requireUser(HttpServletRequest request) {
        var user = request.getAttribute(REQUEST_ATTRIBUTE);
        if (user instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }
        throw new ApiException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED, "Authentication is required");
    }
}
