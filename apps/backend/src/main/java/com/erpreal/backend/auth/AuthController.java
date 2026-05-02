package com.erpreal.backend.auth;

import com.erpreal.backend.audit.AuditService;
import com.erpreal.backend.common.ApiError;
import com.erpreal.backend.room.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final PermissionService permissionService;

    public AuthController(SysUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            PermissionService permissionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.permissionService = permissionService;
    }

    public record LoginRequest(String username, String password, String tenantId) {
    }

    public record LoginResponse(String token, UserInfo user) {
    }

    public record UserInfo(String userId, String displayName, String role,
            String tenantId, String facilityId, Set<String> roles, Set<String> permissions,
            long permissionVersion, boolean superAdmin) {
    }

    private static final String DEFAULT_TENANT_ID = "tenant-yiyang";

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(
                    new ApiError("INVALID_INPUT", "用户名和密码不能为空", UUID.randomUUID().toString()));
        }

        var tenantId = request.tenantId() != null && !request.tenantId().isBlank()
                ? request.tenantId().trim()
                : DEFAULT_TENANT_ID;
        var userOpt = userRepository.findByTenantIdAndUsername(tenantId, request.username().trim());
        if (userOpt.isEmpty()
                || !userOpt.get().isEnabled()
                || !passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash())) {
            logFailedLogin(request.username(), httpRequest);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiError("AUTH_FAILED", "用户名或密码错误", UUID.randomUUID().toString()));
        }

        var user = userOpt.get();
        var token = jwtService.generateToken(user);
        var snapshot = permissionService.snapshot(user.toAuthenticatedUser());
        var userInfo = new UserInfo(user.getId(), user.getDisplayName(),
                user.getRole(), user.getTenantId(), user.getFacilityId(),
                snapshot.roles(), snapshot.permissions(), snapshot.permissionVersion(), snapshot.superAdmin());

        log.info("User logged in: username={}, role={}, tenantId={}", user.getUsername(), user.getRole(), user.getTenantId());
        return ResponseEntity.ok(new LoginResponse(token, userInfo));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        var authedUser = AuthContext.requireUser(request);
        var snapshot = permissionService.snapshot(authedUser);
        var userInfo = new UserInfo(authedUser.userId(), authedUser.displayName(),
                authedUser.role(), authedUser.tenantId(), authedUser.facilityId(),
                snapshot.roles(), snapshot.permissions(), snapshot.permissionVersion(), snapshot.superAdmin());
        return ResponseEntity.ok(userInfo);
    }

    private void logFailedLogin(String username, HttpServletRequest request) {
        var ip = request.getRemoteAddr();
        log.warn("Failed login attempt: username={}, ip={}", username, ip);
        try {
            var ctx = new RequestContext("system", "system", "system", "system", "system",
                    UUID.randomUUID().toString(), null);
            auditService.record(ctx, "auth", "LOGIN_FAILED", "sys_user", username,
                    "IP: " + ip + ", username: " + username);
        } catch (Exception e) {
            log.error("Failed to write login audit log", e);
        }
    }
}
