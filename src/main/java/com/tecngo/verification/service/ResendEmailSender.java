package com.tecngo.verification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;
import java.util.List;
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
        send(recipient, "Verifica tu correo en TecnGo",
                "<p>Hola " + escape(recipientName) + ",</p>"
                        + "<p>Confirma tu correo para activar tu cuenta:</p>"
                        + "<p><a href=\"" + verificationUrl + "\">Verificar correo</a></p>"
                        + "<p>Este enlace expira pronto.</p>",
                "Email verification", verificationUrl);
    }

    @Override
    public void sendPasswordReset(String recipient, String recipientName, String resetUrl) {
        send(recipient, "Recupera tu contraseña de TecnGo",
                "<p>Hola " + escape(recipientName) + ",</p>"
                        + "<p>Recibimos una solicitud para cambiar tu contraseña.</p>"
                        + "<p><a href=\"" + resetUrl + "\">Crear una nueva contraseña</a></p>"
                        + "<p>Si no solicitaste este cambio, ignora este correo.</p>",
                "Password recovery", resetUrl);
    }

    @Override
    public void sendMfaCode(String recipient, String recipientName, String code, long expirationMinutes) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Resend API key is required for administrative MFA");
        }
        send(recipient, "Código de seguridad de TecnGo",
                "<p>Hola " + escape(recipientName) + ",</p>"
                        + "<p>Tu código de acceso administrativo es:</p>"
                        + "<p style=\"font-size:28px;font-weight:bold;letter-spacing:6px\">" + code + "</p>"
                        + "<p>Expira en " + expirationMinutes + " minutos.</p>"
                        + "<p>Si no intentaste ingresar, cambia tu contraseña inmediatamente.</p>",
                "Administrative MFA", "MFA code: " + code);
    }

    @Override
    public void sendDataExport(String recipient, String recipientName, String fileName, byte[] content) {
        send(recipient, "Exportación de datos TecnGo",
                "<p>Hola " + escape(recipientName) + ",</p>"
                        + "<p>Adjuntamos la información solicitada de tu cuenta TecnGo en formato ZIP.</p>"
                        + "<p>Si no solicitaste esta exportación, comunícate con soporte.</p>",
                "Data export", "Attached file: " + fileName, List.of(Map.of(
                        "filename", fileName,
                        "content", Base64.getEncoder().encodeToString(content)
                )));
    }

    @Override
    public void sendTechnicianProfileApproved(String recipient, String recipientName) {
        send(recipient, "¡Bienvenido a la familia TecnGo!",
                "<p>Hola " + escape(recipientName) + ",</p>"
                        + "<p>Tu perfil técnico fue aprobado.</p>"
                        + "<p>Ya estás habilitado para recibir servicios en TecnGo.</p>"
                        + "<p>Entra a la app y revisa las solicitudes cercanas.</p>",
                "Technician profile approval",
                "Technician profile approved");
    }

    private void send(String recipient, String subject, String html, String operation, String fallbackUrl) {
        send(recipient, subject, html, operation, fallbackUrl, List.of());
    }

    private void send(String recipient, String subject, String html, String operation, String fallbackUrl,
                      List<Map<String, String>> attachments) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("{} for {}: {}", operation, recipient, fallbackUrl);
            return;
        }
        log.info("Sending {} email through Resend to {}", operation, recipient);
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("from", from);
            body.put("to", new String[]{recipient});
            body.put("subject", subject);
            body.put("html", html);
            if (!attachments.isEmpty()) {
                body.put("attachments", attachments);
            }
            Map<?, ?> response = restClientBuilder.baseUrl("https://api.resend.com").build()
                    .post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            log.info("Resend accepted {} email for {} with id {}", operation, recipient,
                    response == null ? "unknown" : response.get("id"));
        } catch (RestClientResponseException exception) {
            log.error("Resend rejected {} email for {} with status {}: {}",
                    operation, recipient, exception.getStatusCode(), exception.getResponseBodyAsString());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("{} email delivery failed for {}", operation, recipient, exception);
            throw exception;
        }
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
