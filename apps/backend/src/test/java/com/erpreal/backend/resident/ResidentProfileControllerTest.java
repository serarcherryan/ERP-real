package com.erpreal.backend.resident;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class ResidentProfileControllerTest {
    private static final String PROPERTY_MANAGER_TOKEN = "Bearer dev-property-manager-token";
    private static final String SOCIAL_WORKER_TOKEN = "Bearer dev-social-worker-token";
    private static final String SOCIAL_WORKER_SUPERVISOR_TOKEN = "Bearer dev-social-worker-supervisor-token";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void createsListsAndReturnsResidentDetailFromDatabase() throws Exception {
        var residentId = createResident("CY-2026-API-001", "310101194405126428");

        mockMvc.perform(get("/api/v1/residents")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN)
                        .param("keyword", "CY-2026-API-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].residentNo", is("CY-2026-API-001")))
                .andExpect(jsonPath("$.data.items[0].maskedIdentityNo", is("310101********6428")));

        mockMvc.perform(get("/api/v1/residents/" + residentId)
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(residentId)))
                .andExpect(jsonPath("$.data.maskedIdentityNo", is("310101194405126428")))
                .andExpect(jsonPath("$.data.familyContacts[0].maskedPhone", is("13917223455")))
                .andExpect(jsonPath("$.data.health.fallRiskLevel", is("high")))
                .andExpect(jsonPath("$.data.tags[0]", is("慢病")))
                .andExpect(jsonPath("$.data.createdByName", is("社工主管")))
                .andExpect(jsonPath("$.data.updatedByName", is("社工主管")));
    }

    @Test
    void searchesResidentsByNullableRoomAndLocationFields() throws Exception {
        createResident("CY-2026-API-ROOM", "310101194405126438", "和成养老 - 1栋 - 9楼 - 901专属房");

        mockMvc.perform(get("/api/v1/residents")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN)
                        .param("keyword", "901专属房"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].residentNo", is("CY-2026-API-ROOM")))
                .andExpect(jsonPath("$.data.items[0].room", is("和成养老 - 1栋 - 9楼 - 901专属房")));
    }

    @Test
    void listsResidentsWithoutKeyword() throws Exception {
        createResident("CY-2026-API-NO-KEYWORD", "310101194405126439");

        mockMvc.perform(get("/api/v1/residents")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN)
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].id", notNullValue()));
    }

    @Test
    void masksSensitiveDetailForPropertyRole() throws Exception {
        var residentId = createResident("CY-2026-API-002", "310101194405126429");

        mockMvc.perform(get("/api/v1/residents/" + residentId)
                        .header("Authorization", PROPERTY_MANAGER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maskedIdentityNo", is("310101********6429")))
                .andExpect(jsonPath("$.data.familyContacts[0].maskedPhone", is("139****3455")))
                .andExpect(jsonPath("$.data.householdAddress").doesNotExist());
    }

    @Test
    void rejectsDuplicateResidentNoOrIdentityHash() throws Exception {
        createResident("CY-2026-API-003", "310101194405126430");

        mockMvc.perform(post("/api/v1/residents")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(residentBody("CY-2026-API-003", "310101194405126431")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("RESIDENT_PROFILE_DUPLICATED")));
    }

    @Test
    void replaysIdempotentCreateResidentRequest() throws Exception {
        var body = residentBody("CY-2026-API-004", "310101194405126432");

        mockMvc.perform(post("/api/v1/residents")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN)
                        .header("Idempotency-Key", "resident-create-api-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()));

        mockMvc.perform(post("/api/v1/residents")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN)
                        .header("Idempotency-Key", "resident-create-api-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.residentNo", is("CY-2026-API-004")));
    }

    @Test
    void rejectsWriteForPropertyRoleAndSupportsVersionedUpdate() throws Exception {
        var residentId = createResident("CY-2026-API-005", "310101194405126433");
        var detail = mockMvc.perform(get("/api/v1/residents/" + residentId)
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN))
                .andExpect(status().isOk())
                .andReturn();
        var version = mapper.readTree(detail.getResponse().getContentAsString()).at("/data/version").asLong();
        var updateBody = residentBodyWithVersion("CY-2026-API-005", "310101194405126433", version);

        mockMvc.perform(patch("/api/v1/residents/" + residentId)
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("RESIDENT_PROFILE_FORBIDDEN")));

        mockMvc.perform(patch("/api/v1/residents/" + residentId)
                        .header("Authorization", SOCIAL_WORKER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthSummary", is("更新后的健康摘要")));
    }

    @Test
    void voidsResidentWithSupervisorPermission() throws Exception {
        var residentId = createResident("CY-2026-API-006", "310101194405126434");

        mockMvc.perform(post("/api/v1/residents/" + residentId + "/void")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "重复档案清理"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("Archived")));
    }

    @Test
    void exportTaskIsExplicitlyNotImplemented() throws Exception {
        mockMvc.perform(post("/api/v1/residents/export-tasks")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code", is("RESIDENT_PROFILE_EXPORT_NOT_IMPLEMENTED")));
    }

    private String createResident(String residentNo, String identityNo) throws Exception {
        return createResident(residentNo, identityNo, "和成养老 - 1栋 - 3楼 - 301号房");
    }

    private String createResident(String residentNo, String identityNo, String room) throws Exception {
        var result = mockMvc.perform(post("/api/v1/residents")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(residentBody(residentNo, identityNo, room)))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private String residentBody(String residentNo, String identityNo) {
        return residentBody(residentNo, identityNo, "和成养老 - 1栋 - 3楼 - 301号房");
    }

    private String residentBody(String residentNo, String identityNo, String room) {
        return residentBodyWithVersion(residentNo, identityNo, room, null);
    }

    private String residentBodyWithVersion(String residentNo, String identityNo, Long version) {
        return residentBodyWithVersion(residentNo, identityNo, "和成养老 - 1栋 - 3楼 - 301号房", version);
    }

    private String residentBodyWithVersion(String residentNo, String identityNo, String room, Long version) {
        var versionJson = version == null ? "" : ",\n  \"version\": " + version;
        return """
                {
                  "residentNo": "%s",
                  "name": "陈兰英",
                  "preferredName": "陈阿姨",
                  "gender": "female",
                  "birthDate": "1944-05-12",
                  "identityType": "居民身份证",
                  "identityNo": "%s",
                  "phone": "13821886721",
                  "householdAddress": "上海市黄浦区外滩街道",
                  "currentAddress": "和成养老 3F 护理一区",
                  "admission": {
                    "admissionStatus": "admitted",
                    "admissionDate": "2024-09-12",
                    "contractNo": "HT-2024-0912-001",
                    "zoneId": "zone-hecheng-elderly-care",
                    "buildingId": "building-1",
                    "floorId": "floor-1-3",
                    "roomId": "room-1-3-301",
                    "bedId": "bed-301-a",
                    "room": "%s",
                    "bed": "A床",
                    "nursingZone": "护理一区",
                    "careLevel": "二级护理",
                    "paymentType": "月付",
                    "medicalInsuranceType": "城镇职工医保",
                    "responsibleSocialWorkerId": "staff-001"
                  },
                  "familyContacts": [
                    {
                      "name": "陈思远",
                      "relation": "儿子",
                      "phone": "13917223455",
                      "address": "上海市浦东新区陆家嘴街道",
                      "isEmergency": true,
                      "isGuardian": true,
                      "canReceiveNotice": true,
                      "priority": 1
                    }
                  ],
                  "health": {
                    "bloodType": "A型",
                    "allergyHistory": ["青霉素"],
                    "chronicDiseases": ["高血压", "骨质疏松"],
                    "mobilityLevel": "扶手杖辅助行走",
                    "cognitiveStatus": "轻度记忆下降",
                    "dietRequirement": "低盐软食",
                    "fallRiskLevel": "high",
                    "emergencyPlan": "夜间离床触发巡查"
                  },
                  "healthSummary": "%s",
                  "careNeeds": ["夜间巡查", "跌倒预防", "慢病随访"],
                  "tags": ["重点关注", "慢病"]
                  %s
                }
                """.formatted(
                residentNo,
                identityNo,
                room,
                version == null ? "高血压稳定，需关注夜间睡眠和跌倒风险。" : "更新后的健康摘要",
                versionJson);
    }
}
