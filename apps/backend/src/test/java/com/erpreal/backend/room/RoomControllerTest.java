package com.erpreal.backend.room;

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
class RoomControllerTest {
    private static final String PROPERTY_MANAGER_TOKEN = "Bearer dev-property-manager-token";
    private static final String SOCIAL_WORKER_TOKEN = "Bearer dev-social-worker-token";
    private static final String SOCIAL_WORKER_SUPERVISOR_TOKEN = "Bearer dev-social-worker-supervisor-token";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void returnsHechengRoomTree() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/tree")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zones", hasSize(1)))
                .andExpect(jsonPath("$.data.zones[0].name", is("和成养老")))
                .andExpect(jsonPath("$.data.zones[0].buildings[0].floors[0].rooms[0].locationLabel",
                        is("和成养老 - 1栋 - 3楼 - 301号房")));
    }

    @Test
    void filtersAvailableRooms() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/available")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status", is("available")));
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/tree"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_REQUIRED")));
    }

    @Test
    void rejectsRoomMaintenanceForSocialWorker() throws Exception {
        var body = """
                {
                  "zoneId": "zone-hecheng-elderly-care",
                  "buildingId": "building-1",
                  "floorId": "floor-1-3",
                  "roomNo": "399",
                  "displayName": "399号房",
                  "capacity": 2,
                  "status": "available"
                }
                """;

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", SOCIAL_WORKER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ROOM_FORBIDDEN")));
    }

    @Test
    void rejectsDuplicatedRoomNumberInSameFloor() throws Exception {
        var body = """
                {
                  "zoneId": "zone-hecheng-elderly-care",
                  "buildingId": "building-1",
                  "floorId": "floor-1-3",
                  "roomNo": "301",
                  "displayName": "301号房",
                  "capacity": 2,
                  "status": "available"
                }
                """;

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("ROOM_DUPLICATED")));
    }

    @Test
    void rejectsDuplicatedRoomNumberWhenUpdating() throws Exception {
        var body = """
                {
                  "zoneId": "zone-hecheng-elderly-care",
                  "buildingId": "building-1",
                  "floorId": "floor-1-3",
                  "roomNo": "301",
                  "displayName": "301号房",
                  "capacity": 2,
                  "status": "available",
                  "version": 0
                }
                """;

        mockMvc.perform(patch("/api/v1/rooms/room-2-2-218")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("ROOM_DUPLICATED")));
    }

    @Test
    void rejectsInvalidHierarchy() throws Exception {
        var body = """
                {
                  "zoneId": "zone-hecheng-elderly-care",
                  "buildingId": "building-1",
                  "floorId": "floor-2-2",
                  "roomNo": "399",
                  "displayName": "399号房",
                  "capacity": 2,
                  "status": "available"
                }
                """;

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("ROOM_HIERARCHY_MISMATCH")));
    }

    @Test
    void replaysIdempotentCreateRoomRequest() throws Exception {
        var body = """
                {
                  "zoneId": "zone-hecheng-elderly-care",
                  "buildingId": "building-2",
                  "floorId": "floor-2-2",
                  "roomNo": "299",
                  "displayName": "299号房",
                  "capacity": 1,
                  "status": "available"
                }
                """;

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .header("Idempotency-Key", "room-create-299")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()));

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", PROPERTY_MANAGER_TOKEN)
                        .header("Idempotency-Key", "room-create-299")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomNo", is("299")));
    }

    @Test
    void assignsResidentBedAndUpdatesRoomOccupancy() throws Exception {
        var residentId = createResident("CY-TEST-ROOM-001", "310101194401010011");
        var body = """
                {
                  "residentId": "%s",
                  "bedLabel": "A",
                  "roomVersion": 0
                }
                """.formatted(residentId);

        mockMvc.perform(post("/api/v1/rooms/room-2-2-218/assignments")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.residentId", is(residentId)))
                .andExpect(jsonPath("$.data.roomId", is("room-2-2-218")))
                .andExpect(jsonPath("$.data.occupiedCount", is(1)))
                .andExpect(jsonPath("$.data.roomStatus", is("occupied")));
    }

    private String createResident(String residentNo, String identityNo) throws Exception {
        var body = """
                {
                  "residentNo": "%s",
                  "name": "测试长者",
                  "preferredName": "测试",
                  "gender": "female",
                  "birthDate": "1944-01-01",
                  "identityType": "居民身份证",
                  "identityNo": "%s",
                  "phone": "13800000001",
                  "householdAddress": "上海市黄浦区",
                  "currentAddress": "和成养老",
                  "admission": {
                    "admissionStatus": "admitted",
                    "admissionDate": "2026-04-26",
                    "contractNo": "HT-TEST-001",
                    "zoneId": "zone-hecheng-elderly-care",
                    "buildingId": "building-2",
                    "floorId": "floor-2-2",
                    "roomId": "room-2-2-218",
                    "room": "和成养老 - 2栋 - 2楼 - 218号房",
                    "bed": "A",
                    "nursingZone": "护理一区",
                    "careLevel": "二级护理"
                  },
                  "familyContacts": [
                    {
                      "name": "测试家属",
                      "relation": "女儿",
                      "phone": "13900000001",
                      "address": "上海市浦东新区",
                      "isEmergency": true,
                      "isGuardian": true,
                      "canReceiveNotice": true,
                      "priority": 1
                    }
                  ],
                  "health": {
                    "bloodType": "A型",
                    "allergyHistory": ["青霉素"],
                    "chronicDiseases": ["高血压"],
                    "fallRiskLevel": "medium"
                  },
                  "healthSummary": "测试健康摘要",
                  "careNeeds": ["跌倒预防"],
                  "tags": ["测试"]
                }
                """.formatted(residentNo, identityNo);
        var result = mockMvc.perform(post("/api/v1/residents")
                        .header("Authorization", SOCIAL_WORKER_SUPERVISOR_TOKEN)
                        .header("Idempotency-Key", "create-" + residentNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }
}
