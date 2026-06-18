package com.tecngo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecngo.users.entity.OnboardingStep;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Phase5FlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;

    @Test
    void completeServiceCanBePaidAndRatedOnceWithFinancialHistory() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String clientEmail = "client." + suffix + "@tecngo.local";
        JsonNode client = register("Client " + suffix, clientEmail, "CLIENT");
        JsonNode technician = register("Technician " + suffix, "tech." + suffix + "@tecngo.local", "TECHNICIAN");
        String categoryId = json(mvc.perform(get("/v1/service-categories"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asText();

        mvc.perform(put("/v1/users/me/fcm-token")
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", "fcm-token-" + suffix))))
                .andExpect(status().isNoContent());
        assertThat(users.findByEmailIgnoreCase(clientEmail).orElseThrow().getFcmToken())
                .isEqualTo("fcm-token-" + suffix);
        assertThat(users.findByEmailIgnoreCase(clientEmail).orElseThrow().getFcmTokenUpdatedAt())
                .isNotNull();

        mvc.perform(put("/v1/users/me/profile")
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "fullName", "Client " + suffix,
                                "documentPhotoUrl", "/v1/files/client-document-" + suffix + ".pdf"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING_VERIFICATION"));

        JsonNode profile = json(mvc.perform(post("/v1/technicians/profile")
                        .header("Authorization", bearer(technician))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.ofEntries(
                                Map.entry("documentNumber", "DOC-" + suffix),
                                Map.entry("phone", "3001234567"),
                                Map.entry("categoryIds", List.of(categoryId)),
                                Map.entry("description", "Técnico de integración con experiencia residencial comprobada"),
                                Map.entry("documentPhotoUrl", "/v1/files/document-" + suffix + ".pdf"),
                                Map.entry("workExperienceDescription", "Cinco años de experiencia en instalaciones y reparaciones eléctricas"),
                                Map.entry("latitude", 4.711),
                                Map.entry("longitude", -74.0721),
                                Map.entry("homeAddress", "Calle 10 # 20-30"),
                                Map.entry("homeLatitude", 4.711),
                                Map.entry("homeLongitude", -74.0721)
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        JsonNode admin = login("admin@tecngo.local", "Admin123!");
        mvc.perform(put("/v1/verifications/{id}/verify", technician.get("userId").asText())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));
        mvc.perform(put("/v1/admin/technicians/{id}/approve", profile.get("id").asText())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        var technicianUser = users.findById(java.util.UUID.fromString(technician.get("userId").asText())).orElseThrow();
        technicianUser.setEmailVerified(true);
        technicianUser.setOnboardingCompleted(true);
        technicianUser.setOnboardingStep(OnboardingStep.COMPLETED);
        users.save(technicianUser);
        technician = login("tech." + suffix + "@tecngo.local", "TecnGo123!");

        JsonNode request = json(mvc.perform(post("/v1/service-requests")
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "categoryId", categoryId,
                                "description", "Servicio Fase 4",
                                "address", "Bogotá",
                                "latitude", 4.712,
                                "longitude", -74.073,
                                "estimatedPrice", 120000
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("QUOTE_PENDING"))
                .andReturn().getResponse().getContentAsString());
        String requestId = request.get("id").asText();

        JsonNode quote = json(mvc.perform(put("/v1/service-requests/{id}/quote", requestId)
                        .header("Authorization", bearer(technician))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("technicianPrice", 150000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString());

        JsonNode clientNotifications = json(mvc.perform(get("/v1/notifications")
                .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("NEW_QUOTE"))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(get("/v1/notifications/unread-count")
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
        mvc.perform(put("/v1/notifications/{id}/read", clientNotifications.get(0).get("id").asText())
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
        mvc.perform(get("/v1/notifications/unread-count")
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mvc.perform(put("/v1/service-requests/{id}/confirm-quote", requestId)
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("quoteId", quote.get("id").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUOTE_ACCEPTED"));

        mvc.perform(post("/v1/service-requests/{id}/chat/messages", requestId)
                        .header("Authorization", bearer(technician))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("message", "Voy a revisar el servicio"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Voy a revisar el servicio"));
        mvc.perform(get("/v1/service-requests/{id}/chat", requestId)
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].readAt").doesNotExist());
        mvc.perform(put("/v1/service-requests/{id}/chat/read", requestId)
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1));
        mvc.perform(get("/v1/service-requests/{id}/chat", requestId)
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].readAt").isNotEmpty());

        mvc.perform(post("/v1/service-requests/{id}/payment/cash", requestId)
                        .header("Authorization", bearer(client)))
                .andExpect(status().isConflict());

        updateStatus(requestId, technician, "ON_THE_WAY");
        updateStatus(requestId, technician, "ARRIVED");
        updateStatus(requestId, technician, "IN_PROGRESS");
        updateStatus(requestId, technician, "COMPLETED");

        mvc.perform(post("/v1/service-requests/{id}/payment/cash", requestId)
                        .header("Authorization", bearer(client)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(150000))
                .andExpect(jsonPath("$.platformFee").value(0))
                .andExpect(jsonPath("$.technicianAmount").value(150000))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"));

        mvc.perform(get("/v1/payments/mine")
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceRequestId").value(requestId));

        mvc.perform(get("/v1/technicians/me/earnings")
                .header("Authorization", bearer(technician)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTechnicianAmount").value(150000))
                .andExpect(jsonPath("$.paymentCount").value(1));

        mvc.perform(get("/v1/admin/payments")
                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlatformFee").value(0));

        mvc.perform(post("/v1/service-requests/{id}/ratings", requestId)
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("score", 5, "comment", "Excelente servicio"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(5));

        mvc.perform(post("/v1/service-requests/{id}/ratings", requestId)
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("score", 4, "comment", "Duplicada"))))
                .andExpect(status().isConflict());

        mvc.perform(post("/v1/service-requests/{id}/ratings", requestId)
                        .header("Authorization", bearer(technician))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("score", 4, "comment", "Cliente puntual"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ratedUserId").value(client.get("userId").asText()));

        mvc.perform(get("/v1/users/me/profile")
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.completedServicesCount").value(1))
                .andExpect(jsonPath("$.paidServicesCount").value(1));

        mvc.perform(get("/v1/technicians/{id}/ratings", technician.get("userId").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comment").value("Excelente servicio"));

        mvc.perform(get("/v1/technicians/{id}/summary", technician.get("userId").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(5.0))
                .andExpect(jsonPath("$.ratingCount").value(1));
    }

    private JsonNode register(String fullName, String email, String role) throws Exception {
        return json(mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("fullName", fullName, "email", email,
                                "password", "TecnGo123!", "confirmPassword", "TecnGo123!",
                                "role", role))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationStatus").value("CREATED"))
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode login(String email, String password) throws Exception {
        return json(mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private void updateStatus(String id, JsonNode session, String status) throws Exception {
        mvc.perform(put("/v1/service-requests/{id}/status", id)
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("status", status))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status));
    }

    private String body(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(JsonNode session) {
        return "Bearer " + session.get("token").asText();
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
