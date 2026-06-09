package com.tecngo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Phase6AProfileValidationIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void registrationRequiresDocumentAndTechnicianExperience() throws Exception {
        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Cliente sin documento","email":"missing-doc@tecngo.local",
                                "password":"TecnGo123!","role":"CLIENT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.documentPhotoUrl").exists());

        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Técnico sin experiencia","email":"missing-exp@tecngo.local",
                                "password":"TecnGo123!","role":"TECHNICIAN",
                                "documentPhotoUrl":"/v1/files/document.pdf"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadAcceptsAllowedFilesAndRejectsOtherTypes() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "profile.png",
                "image/png", new byte[]{1, 2, 3});
        mvc.perform(multipart("/v1/files/upload").file(image))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/v1/files/")));

        MockMultipartFile executable = new MockMultipartFile("file", "bad.exe",
                "application/octet-stream", new byte[]{1});
        mvc.perform(multipart("/v1/files/upload").file(executable))
                .andExpect(status().isBadRequest());
    }
}
