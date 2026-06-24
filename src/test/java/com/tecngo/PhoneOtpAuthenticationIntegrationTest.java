package com.tecngo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecngo.phone_auth.entity.PhoneOtpVerification;
import com.tecngo.phone_auth.repository.PhoneOtpVerificationRepository;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhoneOtpAuthenticationIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository users;
    @Autowired
    private PhoneOtpVerificationRepository verifications;

    @Test
    void verifiesOtpRegistersPhoneOnlyUserAndLogsIn() throws Exception {
        String phone = "300" + String.valueOf(System.nanoTime()).substring(4, 11);

        String sendBody = mvc.perform(post("/v1/auth/phone/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("phone", phone))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debugCode").value("00000"))
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(sendBody).get("expiresAt").asText()).isNotBlank();

        String verifyBody = mvc.perform(post("/v1/auth/phone/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "phone", phone, "code", "00000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andReturn().getResponse().getContentAsString();
        String verificationToken = objectMapper.readTree(verifyBody).get("verificationToken").asText();

        String registerBody = mvc.perform(post("/v1/auth/register-by-phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "fullName", "Usuario celular",
                                "phone", phone,
                                "verificationToken", verificationToken,
                                "password", "TecnGo123!",
                                "confirmPassword", "TecnGo123!",
                                "role", "CLIENT"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phoneVerified").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode registered = objectMapper.readTree(registerBody);
        String normalized = "+57" + phone;
        assertThat(users.findByPhoneNormalized(normalized)).isPresent()
                .get().satisfies(user -> {
                    assertThat(user.getEmail()).isNull();
                    assertThat(user.getPhone()).isEqualTo(phone);
                    assertThat(user.isPhoneVerified()).isTrue();
                });

        mvc.perform(post("/v1/auth/login-by-phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "phone", phone, "password", "TecnGo123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(registered.get("userId").asText()))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void rejectsExpiredOtpAndPersistsFailedAttempts() throws Exception {
        String phone = "301" + String.valueOf(System.nanoTime()).substring(5, 12);
        String normalized = "+57" + phone;
        mvc.perform(post("/v1/auth/phone/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("phone", phone))))
                .andExpect(status().isOk());

        mvc.perform(post("/v1/auth/phone/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "phone", phone, "code", "11111"))))
                .andExpect(status().isConflict());

        PhoneOtpVerification latest = verifications
                .findFirstByPhoneAndVerifiedFalseOrderByCreatedAtDesc(normalized).orElseThrow();
        assertThat(latest.getAttempts()).isEqualTo(1);

        latest.setExpiresAt(Instant.now().minusSeconds(1));
        verifications.saveAndFlush(latest);
        mvc.perform(post("/v1/auth/phone/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "phone", phone, "code", "00000"))))
                .andExpect(status().isConflict());
    }
}
