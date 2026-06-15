package com.tecngo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFailureIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void invalidCredentialsReturnUnauthorizedInsteadOfForbidden() throws Exception {
        mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@tecngo.test",
                                  "password": "Incorrect123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));
    }

    @Test
    void publicLegalDocumentsDoNotRequireAuthentication() throws Exception {
        mvc.perform(get("/v1/legal/documents/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void forgotPasswordAlwaysReturnsGenericMessage() throws Exception {
        mvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@tecngo.test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña."));
    }

    @Test
    void registrationRejectsDifferentPasswordConfirmation() throws Exception {
        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Registro inválido",
                                  "email":"mismatch@tecngo.test",
                                  "password":"TecnGo123!",
                                  "confirmPassword":"Different123!",
                                  "role":"CLIENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Las contraseñas no coinciden"));
    }
}
