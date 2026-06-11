package com.tecngo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MultipleQuotesIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void multipleTechniciansCanQuoteUntilClientAcceptsOne() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        JsonNode admin = login("admin@tecngo.local", "Admin123!");
        JsonNode client = register("Client " + suffix, "multi-client-" + suffix + "@tecngo.test", "CLIENT");
        String categoryId = json(mvc.perform(get("/v1/service-categories"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asText();

        mvc.perform(put("/v1/users/me/profile")
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "fullName", "Client " + suffix,
                                "documentPhotoUrl", "/v1/files/client-" + suffix + ".pdf"
                        ))))
                .andExpect(status().isOk());

        JsonNode first = approvedTechnician("First " + suffix, "first-" + suffix + "@tecngo.test",
                "DOC-A-" + suffix, categoryId, 4.711, -74.0721, admin);
        JsonNode second = approvedTechnician("Second " + suffix, "second-" + suffix + "@tecngo.test",
                "DOC-B-" + suffix, categoryId, 4.7115, -74.0725, admin);

        String requestId = json(mvc.perform(post("/v1/service-requests")
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "categoryId", categoryId,
                                "description", "Comparar varias cotizaciones",
                                "address", "Bogotá, Colombia",
                                "latitude", 4.712,
                                "longitude", -74.073,
                                "estimatedPrice", 100000
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(get("/v1/notifications").header("Authorization", bearer(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("NEW_REQUEST"));
        mvc.perform(get("/v1/notifications").header("Authorization", bearer(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("NEW_REQUEST"));

        JsonNode firstQuote = quote(requestId, first, 120000, "Primera oferta");

        mvc.perform(get("/v1/service-requests/available?radiusKm=10")
                        .header("Authorization", bearer(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + requestId + "')]").isNotEmpty());

        JsonNode secondQuote = quote(requestId, second, 110000, "Segunda oferta");

        mvc.perform(get("/v1/service-requests/{id}/quotes", requestId)
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));

        mvc.perform(put("/v1/service-requests/{id}/confirm-quote", requestId)
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("quoteId", secondQuote.get("id").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUOTE_ACCEPTED"))
                .andExpect(jsonPath("$.technicianId").value(second.get("userId").asText()))
                .andExpect(jsonPath("$.finalPrice").value(110000));

        mvc.perform(get("/v1/service-requests/{id}/quotes", requestId)
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstQuote.get("id").asText()))
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[1].id").value(secondQuote.get("id").asText()))
                .andExpect(jsonPath("$[1].status").value("ACCEPTED"));

        mvc.perform(put("/v1/service-requests/{id}/quote", requestId)
                        .header("Authorization", bearer(first))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("technicianPrice", 90000))))
                .andExpect(status().isConflict());
    }

    private JsonNode approvedTechnician(String name, String email, String document, String categoryId,
                                        double latitude, double longitude, JsonNode admin) throws Exception {
        JsonNode session = register(name, email, "TECHNICIAN");
        JsonNode profile = json(mvc.perform(post("/v1/technicians/profile")
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "documentNumber", document,
                                "phone", "3001234567",
                                "categoryIds", List.of(categoryId),
                                "description", "Técnico disponible",
                                "documentPhotoUrl", "/v1/files/" + document + ".pdf",
                                "workExperienceDescription", "Experiencia comprobada",
                                "latitude", latitude,
                                "longitude", longitude
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        mvc.perform(put("/v1/verifications/{id}/verify", session.get("userId").asText())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mvc.perform(put("/v1/admin/technicians/{id}/approve", profile.get("id").asText())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        return session;
    }

    private JsonNode quote(String requestId, JsonNode technician, int price, String description) throws Exception {
        return json(mvc.perform(put("/v1/service-requests/{id}/quote", requestId)
                        .header("Authorization", bearer(technician))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("technicianPrice", price, "description", description))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode register(String name, String email, String role) throws Exception {
        return json(mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("fullName", name, "email", email,
                                "password", "TecnGo123!", "role", role))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode login(String email, String password) throws Exception {
        return json(mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private String body(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private JsonNode json(String value) throws Exception { return mapper.readTree(value); }
    private String bearer(JsonNode session) { return "Bearer " + session.get("token").asText(); }
}
