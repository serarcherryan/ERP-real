package com.erpreal.backend.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Duration TOKEN_VALIDITY = Duration.ofHours(8);

    private final SecretKey key;

    public JwtService(@Value("${erp.auth.jwt-secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(SysUserEntity user) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(user.getId())
                .claim("displayName", user.getDisplayName())
                .claim("role", user.getRole())
                .claim("tenantId", user.getTenantId())
                .claim("facilityId", user.getFacilityId())
                .claim("permissionVersion", user.getPermissionVersion())
                .claim("superAdmin", user.isSuperAdmin())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_VALIDITY)))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            var permissionVersion = claims.get("permissionVersion", Number.class);
            return new AuthenticatedUser(
                    claims.getSubject(),
                    claims.get("displayName", String.class),
                    claims.get("tenantId", String.class),
                    claims.get("facilityId", String.class),
                    claims.get("role", String.class),
                    permissionVersion == null ? 1 : permissionVersion.longValue(),
                    Boolean.TRUE.equals(claims.get("superAdmin", Boolean.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
