package com.tecngo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.require-persisted-sessions=true",
        "app.security.admin-mfa-enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductionSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void logoutRevokesThePersistedJwtSession() throws Exception {
        String email = "session-" + UUID.randomUUID() + "@tecngo.local";
        String body = mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Session User",
                                "email", email,
                                "password", "TecnGo123!",
                                "confirmPassword", "TecnGo123!",
                                "role", "CLIENT"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode session = objectMapper.readTree(body);
        String bearer = "Bearer " + session.get("token").asText();

        mvc.perform(get("/v1/users/me/profile").header("Authorization", bearer))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/auth/logout").header("Authorization", bearer))
                .andExpect(status().isNoContent());
        mvc.perform(get("/v1/users/me/profile").header("Authorization", bearer))
                .andExpect(status().isForbidden());
    }

    @Test
    void swaggerRequiresAnAuthenticatedAdministrator() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isForbidden());
    }
}
