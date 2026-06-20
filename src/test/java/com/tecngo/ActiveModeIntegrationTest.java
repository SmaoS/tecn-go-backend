package com.tecngo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecngo.auth.service.JwtService;
import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.Role;
import com.tecngo.users.repository.UserActiveModeAuditRepository;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActiveModeIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository users;
    @Autowired
    private UserActiveModeAuditRepository audits;
    @Autowired
    private JwtService jwtService;

    @Test
    void changesModeReturnsNewTokenAndCreatesAudit() throws Exception {
        String email = "dual-mode-" + UUID.randomUUID() + "@tecngo.local";
        JsonNode session = register(email);
        var user = users.findByEmailIgnoreCase(email).orElseThrow();
        user.addRole(Role.TECHNICIAN);
        users.saveAndFlush(user);

        String responseBody = mvc.perform(put("/v1/users/me/active-mode")
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode":"TECHNICIAN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.roles[0]").exists())
                .andExpect(jsonPath("$.activeMode").value("TECHNICIAN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        assertThat(jwtService.extractActiveMode(response.get("token").asText())).isEqualTo("TECHNICIAN");
        assertThat(jwtService.extractRoles(response.get("token").asText()))
                .containsExactly("CLIENT", "TECHNICIAN");
        assertThat(users.findById(user.getId()).orElseThrow().getActiveMode())
                .isEqualTo(ActiveMode.TECHNICIAN);
        assertThat(audits.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .singleElement()
                .satisfies(audit -> {
                    assertThat(audit.getPreviousMode()).isEqualTo(ActiveMode.CLIENT);
                    assertThat(audit.getNewMode()).isEqualTo(ActiveMode.TECHNICIAN);
                    assertThat(audit.getReason()).isEqualTo("USER_MODE_CHANGE");
                });
    }

    @Test
    void rejectsModeWhenAccountDoesNotHaveCapability() throws Exception {
        JsonNode session = register("single-mode-" + UUID.randomUUID() + "@tecngo.local");

        mvc.perform(put("/v1/users/me/active-mode")
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode":"TECHNICIAN"}
                                """))
                .andExpect(status().isForbidden());
    }

    private JsonNode register(String email) throws Exception {
        String body = mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "fullName", "Usuario multirrol",
                                "email", email,
                                "password", "TecnGo123!",
                                "confirmPassword", "TecnGo123!",
                                "role", "CLIENT"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String bearer(JsonNode session) {
        return "Bearer " + session.get("token").asText();
    }
}
