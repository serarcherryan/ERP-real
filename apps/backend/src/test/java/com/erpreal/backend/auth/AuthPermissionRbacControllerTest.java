package com.erpreal.backend.auth;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthPermissionRbacControllerTest {
    private static final String PROPERTY_MANAGER_TOKEN = "Bearer dev-property-manager-token";
    private static final String PROPERTY_SUPERVISOR_TOKEN = "Bearer dev-property-supervisor-token";
    private static final String SOCIAL_WORKER_TOKEN = "Bearer dev-social-worker-token";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void adminLoginReturnsAllPermissions() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "Erp@2026"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.superAdmin").value(true))
                .andExpect(jsonPath("$.user.permissions", hasItem(PermissionCodes.IDENTITY_ROLE_UPDATE_PERMISSIONS)))
                .andExpect(jsonPath("$.user.permissions", hasItem(PermissionCodes.RESIDENT_PROFILE_DELETE)));
    }

    @Test
    void socialWorkerMeReturnsOnlyResidentCreateAndRead() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", SOCIAL_WORKER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions", hasItem(PermissionCodes.RESIDENT_PROFILE_CREATE)))
                .andExpect(jsonPath("$.permissions", hasItem(PermissionCodes.RESIDENT_PROFILE_READ)))
                .andExpect(jsonPath("$.permissions", not(hasItem(PermissionCodes.RESIDENT_PROFILE_UPDATE))))
                .andExpect(jsonPath("$.permissions", not(hasItem(PermissionCodes.IDENTITY_USER_CREATE))));
    }

    @Test
    void propertyManagerCreatesUserAndSocialWorkerIsForbidden() throws Exception {
        var roleId = roleId("social-worker");

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", SOCIAL_WORKER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody("forbidden_user", roleId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("IDENTITY_FORBIDDEN"));

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody("new_social_worker", roleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.username").value("new_social_worker"))
                .andExpect(jsonPath("$.data.roles", hasItem("social-worker")))
                .andExpect(jsonPath("$.data.permissions", hasItem(PermissionCodes.RESIDENT_PROFILE_READ)));
    }

    @Test
    void rolePermissionUpdateAffectsResidentAuthorization() throws Exception {
        var roleId = roleId("property-supervisor");
        var residentId = createResident("CY-RBAC-001", "310101194405126501");
        var detail = mockMvc.perform(get("/api/v1/residents/" + residentId)
                        .header("Authorization", PROPERTY_MANAGER_TOKEN))
                .andExpect(status().isOk())
                .andReturn();
        var version = mapper.readTree(detail.getResponse().getContentAsString()).at("/data/version").asLong();

        mockMvc.perform(put("/api/v1/roles/" + roleId + "/permissions")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionCodes": [
                                    "resident.profile:create",
                                    "resident.profile:read",
                                    "resident.profile:delete"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCodes", not(hasItem(PermissionCodes.RESIDENT_PROFILE_UPDATE))));

        mockMvc.perform(patch("/api/v1/residents/" + residentId)
                        .header("Authorization", PROPERTY_SUPERVISOR_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(residentBody("CY-RBAC-001", "310101194405126501", version)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESIDENT_PROFILE_FORBIDDEN"));

        restorePropertySupervisorPermissions(roleId);
    }

    @Test
    void rejectsUnknownPermissionCode() throws Exception {
        var roleId = roleId("property-supervisor");

        mockMvc.perform(put("/api/v1/roles/" + roleId + "/permissions")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissionCodes": ["missing.permission:code"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDENTITY_PERMISSION_NOT_FOUND"));
    }

    @Test
    void disableUserRejectsDisablingSuperAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/users/user-admin/disable")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("IDENTITY_SUPER_ADMIN_PROTECTED"));
    }

    @Test
    void disableUserRejectsDisablingSelf() throws Exception {
        mockMvc.perform(post("/api/v1/users/user-prop-manager/disable")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("IDENTITY_SELF_DISABLE"));
    }

    @Test
    void replaceUserRolesRejectsModifyingSelf() throws Exception {
        var roleId = roleId("social-worker");
        
        mockMvc.perform(put("/api/v1/users/user-prop-manager/roles")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleIds": ["%s"]
                                }
                                """.formatted(roleId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("IDENTITY_SELF_ROLE_CHANGE"));
    }

    private String roleId(String roleCode) throws Exception {
        var result = mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN))
                .andExpect(status().isOk())
                .andReturn();
        var roles = mapper.readTree(result.getResponse().getContentAsString()).at("/data");
        return StreamSupport.stream(roles.spliterator(), false)
                .filter(role -> roleCode.equals(role.get("code").asText()))
                .findFirst()
                .map(role -> role.get("id").asText())
                .orElseThrow();
    }

    private String createUserBody(String username, String roleId) {
        return """
                {
                  "username": "%s",
                  "password": "Erp@2026",
                  "displayName": "新社工",
                  "roleIds": ["%s"]
                }
                """.formatted(username, roleId);
    }

    private String createResident(String residentNo, String identityNo) throws Exception {
        var result = mockMvc.perform(post("/api/v1/residents")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(residentBody(residentNo, identityNo, null)))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private String residentBody(String residentNo, String identityNo, Long version) {
        var versionJson = version == null ? "" : ",\n  \"version\": " + version;
        return """
                {
                  "residentNo": "%s",
                  "name": "权限测试长者",
                  "preferredName": "权限测试",
                  "gender": "female",
                  "birthDate": "1944-05-12",
                  "identityType": "居民身份证",
                  "identityNo": "%s",
                  "phone": "13821886721",
                  "householdAddress": "上海市黄浦区",
                  "currentAddress": "和成养老",
                  "admission": {
                    "admissionStatus": "admitted",
                    "admissionDate": "2024-09-12",
                    "zoneId": "zone-hecheng-elderly-care",
                    "buildingId": "building-1",
                    "floorId": "floor-1-3",
                    "roomId": "room-1-3-301",
                    "room": "和成养老 - 1栋 - 3楼 - 301号房",
                    "bed": "A床"
                  },
                  "familyContacts": [
                    {
                      "name": "测试家属",
                      "relation": "女儿",
                      "phone": "13917223455",
                      "isEmergency": true,
                      "isGuardian": true,
                      "canReceiveNotice": true,
                      "priority": 1
                    }
                  ],
                  "health": {"fallRiskLevel": "low"},
                  "healthSummary": "权限测试健康摘要",
                  "careNeeds": ["权限测试"],
                  "tags": ["权限测试"]
                  %s
                }
                """.formatted(residentNo, identityNo, versionJson);
    }

    private void restorePropertySupervisorPermissions(String roleId) throws Exception {
        mockMvc.perform(put("/api/v1/roles/" + roleId + "/permissions")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionCodes": [
                                    "resident.profile:create",
                                    "resident.profile:read",
                                    "resident.profile:update",
                                    "resident.profile:delete"
                                  ]
                                }
                                """))
                .andExpect(status().isOk());
    }
}
