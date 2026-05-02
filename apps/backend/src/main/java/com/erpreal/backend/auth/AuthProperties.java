package com.erpreal.backend.auth;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "erp.auth")
public class AuthProperties {
    private Map<String, TokenUser> tokens = new HashMap<>();

    public Map<String, TokenUser> getTokens() {
        return tokens;
    }

    public void setTokens(Map<String, TokenUser> tokens) {
        this.tokens = tokens;
    }

    public static class TokenUser {
        private String userId;
        private String displayName;
        private String tenantId;
        private String facilityId;
        private String role;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getFacilityId() {
            return facilityId;
        }

        public void setFacilityId(String facilityId) {
            this.facilityId = facilityId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        AuthenticatedUser toUser() {
            return new AuthenticatedUser(userId, displayName, tenantId, facilityId, role, 1, false);
        }
    }
}
