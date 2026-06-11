package com.tecngo.notifications.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
public class FirebaseConfig {
    @Bean
    FirebaseApp firebaseApp(@Value("${app.firebase.project-id}") String projectId,
                            @Value("${app.firebase.private-key}") String privateKey,
                            @Value("${app.firebase.client-email}") String clientEmail,
                            @Value("${app.firebase.client-id}") String clientId,
                            @Value("${app.firebase.private-key-id}") String privateKeyId,
                            @Value("${app.firebase.credentials-base64}") String credentialsBase64) throws Exception {
        byte[] credentials = credentialsBase64 == null || credentialsBase64.isBlank()
                ? buildCredentials(projectId, privateKey, clientEmail, clientId, privateKeyId)
                : Base64.getDecoder().decode(credentialsBase64.trim());
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(credentials)))
                .setProjectId(projectId)
                .build();
        return FirebaseApp.initializeApp(options);
    }

    private byte[] buildCredentials(String projectId, String privateKey, String clientEmail,
                                    String clientId, String privateKeyId) {
        String json = """
                {
                  "type": "service_account",
                  "project_id": "%s",
                  "private_key_id": "%s",
                  "private_key": "%s",
                  "client_email": "%s",
                  "client_id": "%s",
                  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """.formatted(
                escape(projectId),
                escape(privateKeyId),
                escape(privateKey.replace("\\n", "\n")),
                escape(clientEmail),
                escape(clientId));
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
