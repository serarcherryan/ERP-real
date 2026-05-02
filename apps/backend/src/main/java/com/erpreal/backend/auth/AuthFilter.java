package com.erpreal.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.erpreal.backend.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private final AuthProperties properties;
    private final JwtService jwtService;
    private final ObjectMapper mapper;

    public AuthFilter(AuthProperties properties, JwtService jwtService, ObjectMapper mapper) {
        this.properties = properties;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var uri = request.getRequestURI();
        return uri.startsWith("/actuator/")
                || uri.equals("/api/v1/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var token = bearerToken(request);
        if (token == null) {
            writeUnauthorized(response);
            return;
        }

        // 1. Try JWT first
        var jwtUser = jwtService.parseToken(token);
        if (jwtUser != null) {
            request.setAttribute(AuthContext.REQUEST_ATTRIBUTE, jwtUser);
            chain.doFilter(request, response);
            return;
        }

        // 2. Fallback to dev static token
        var tokenUser = properties.getTokens().get(token);
        if (tokenUser != null) {
            request.setAttribute(AuthContext.REQUEST_ATTRIBUTE, tokenUser.toUser());
            chain.doFilter(request, response);
            return;
        }

        writeUnauthorized(response);
    }

    private String bearerToken(HttpServletRequest request) {
        var authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getWriter(),
                new ApiError("AUTH_REQUIRED", "Authentication is required", UUID.randomUUID().toString()));
    }
}
