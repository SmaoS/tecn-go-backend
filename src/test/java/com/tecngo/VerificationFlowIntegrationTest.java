package com.tecngo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VerificationFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void adminCreatesVerifierAndVerifierApprovesPendingIdentity() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        JsonNode admin = login("admin@tecngo.local", "Admin123!");
        JsonNode verifier = objectMapper.readTree(mvc.perform(post("/v1/admin/verifiers")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Verificador %s","email":"verifier-%s@tecngo.local",
                                "password":"TecnGo123!"}
                                """.formatted(suffix, suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("verifier-" + suffix + "@tecngo.local"))
                .andReturn().getResponse().getContentAsString());

        JsonNode verifierSession = login(verifier.get("email").asText(), "TecnGo123!");
        JsonNode client = registerClient(suffix);
        mvc.perform(put("/v1/users/me/profile")
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Cliente verificación",
                                "documentPhotoUrl":"/v1/files/private-verification.pdf"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING_VERIFICATION"));

        mvc.perform(get("/v1/verifications/pending")
                        .header("Authorization", bearer(verifierSession)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(client.get("userId").asText())).exists());

        mvc.perform(put("/v1/verifications/{id}/verify", client.get("userId").asText())
                        .header("Authorization", bearer(verifierSession)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));
    }

    private JsonNode registerClient(String suffix) throws Exception {
        return objectMapper.readTree(mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Cliente verificación","email":"verify-%s@tecngo.local",
                                "password":"TecnGo123!","confirmPassword":"TecnGo123!","role":"CLIENT"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode login(String email, String password) throws Exception {
        return objectMapper.readTree(mvc.perform(post("/v1/auth/login")
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
