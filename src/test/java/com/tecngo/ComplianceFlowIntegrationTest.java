package com.tecngo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.security.require-persisted-sessions=true")
@AutoConfigureMockMvc
class ComplianceFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void userCanExportDataWithoutPasswordOrTokens() throws Exception {
        JsonNode session = registerClient("export");
        String body = mvc.perform(post("/v1/users/me/data-export")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.type").value("EXPORT"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContainIgnoringCase("password")
                .doesNotContainIgnoringCase("fcm_token")
                .doesNotContainIgnoringCase("secure_url")
                .doesNotContainIgnoringCase("public_id");

        mvc.perform(post("/v1/users/me/data-export-request")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isConflict());
    }

    @Test
    void userCanCreateOnlyOnePendingAnonymizationRequest() throws Exception {
        JsonNode session = registerClient("anonymize");
        String authorization = bearer(session);

        mvc.perform(post("/v1/users/me/data-anonymization")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Ya no deseo usar la plataforma\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ANONYMIZATION"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mvc.perform(post("/v1/users/me/data-anonymization")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Solicitud duplicada\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanApproveDataExportAndSendFile() throws Exception {
        JsonNode session = registerClient("approved-export");
        String requestBody = mvc.perform(post("/v1/users/me/data-export-request")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String requestId = mapper.readTree(requestBody).get("id").asText();
        JsonNode admin = login("admin@tecngo.local", "Admin123!");

        mvc.perform(put("/v1/admin/data-export-requests/{id}/approve", requestId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.exportFileUrl").isNotEmpty())
                .andExpect(jsonPath("$.sentAt").isNotEmpty());

        mvc.perform(get("/v1/users/me/data-export-requests")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    @Test
    void adminCanApproveAnonymizationAndRevokeUserAccess() throws Exception {
        JsonNode session = registerClient("approved-anonymization");
        String requestBody = mvc.perform(post("/v1/users/me/data-anonymization")
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Ejercicio del derecho de supresión\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String requestId = mapper.readTree(requestBody).get("id").asText();
        JsonNode admin = login("admin@tecngo.local", "Admin123!");

        mvc.perform(put("/v1/admin/compliance/data-requests/{id}/approve-anonymization", requestId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(post("/v1/users/me/data-export")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isForbidden());
    }

    private JsonNode registerClient(String prefix) throws Exception {
        return mapper.readTree(mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Cliente cumplimiento","email":"%s-%s@tecngo.local",
                                "password":"TecnGo123!","confirmPassword":"TecnGo123!","role":"CLIENT"}
                                """.formatted(prefix, System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode login(String email, String password) throws Exception {
        return mapper.readTree(mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private String bearer(JsonNode session) {
        return "Bearer " + session.get("token").asText();
    }
}
