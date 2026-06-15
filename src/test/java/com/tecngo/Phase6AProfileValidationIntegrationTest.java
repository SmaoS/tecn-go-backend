package com.tecngo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Phase6AProfileValidationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void basicRegistrationStartsCreatedAndDocumentMovesToPendingVerification() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        JsonNode session = objectMapper.readTree(mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Cliente básico","email":"basic-%s@tecngo.local",
                                "password":"TecnGo123!","confirmPassword":"TecnGo123!","role":"CLIENT"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationStatus").value("CREATED"))
                .andReturn().getResponse().getContentAsString());

        mvc.perform(put("/v1/users/me/profile")
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Cliente básico",
                                "documentPhotoUrl":"/v1/files/private-document.pdf"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING_VERIFICATION"));
    }

    @Test
    void uploadAcceptsAllowedFilesAndRejectsOtherTypes() throws Exception {
        JsonNode session = registerClient();
        MockMultipartFile image = new MockMultipartFile("file", "profile.png",
                "image/png", new byte[]{1, 2, 3});
        mvc.perform(multipart("/v1/files/upload").file(image)
                        .header("Authorization", bearer(session)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/v1/files/")));

        MockMultipartFile executable = new MockMultipartFile("file", "bad.exe",
                "application/octet-stream", new byte[]{1});
        mvc.perform(multipart("/v1/files/upload").file(executable)
                        .header("Authorization", bearer(session)))
                .andExpect(status().isBadRequest());
    }

    private JsonNode registerClient() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        return objectMapper.readTree(mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Cliente archivos","email":"files-%s@tecngo.local",
                                "password":"TecnGo123!","confirmPassword":"TecnGo123!","role":"CLIENT"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private String bearer(JsonNode session) {
        return "Bearer " + session.get("token").asText();
    }
}
