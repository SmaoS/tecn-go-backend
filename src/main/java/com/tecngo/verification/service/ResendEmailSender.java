package com.tecngo.verification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResendEmailSender implements EmailSender {
    private final RestClient.Builder restClientBuilder;

    @Value("${app.email.resend-api-key:}")
    private String apiKey;

    @Value("${app.email.from:TecnGo <onboarding@resend.dev>}")
    private String from;

    @Override
    public void sendVerification(String recipient, String recipientName, String verificationUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Email verification for {}: {}", recipient, verificationUrl);
            return;
        }
        restClientBuilder.baseUrl("https://api.resend.com").build()
                .post()
                .uri("/emails")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "from", from,
                        "to", new String[]{recipient},
                        "subject", "Verifica tu correo en TecnGo",
                        "html", "<p>Hola " + escape(recipientName) + ",</p>"
                                + "<p>Confirma tu correo para activar tu cuenta:</p>"
                                + "<p><a href=\"" + verificationUrl + "\">Verificar correo</a></p>"
                                + "<p>Este enlace expira pronto.</p>"
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
