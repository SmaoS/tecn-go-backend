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

@Configuration
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
public class FirebaseConfig {
    @Bean
    FirebaseApp firebaseApp(@Value("${app.firebase.project-id}") String projectId,
                            @Value("${app.firebase.private-key}") String privateKey,
                            @Value("${app.firebase.client-email}") String clientEmail) throws Exception {
        String credentials = """
                {
                  "type": "service_account",
                  "project_id": "%s",
                  "private_key": "%s",
                  "client_email": "%s",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """.formatted(escape(projectId), escape(privateKey.replace("\\n", "\n")), escape(clientEmail));
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(
                        new ByteArrayInputStream(credentials.getBytes(StandardCharsets.UTF_8))))
                .setProjectId(projectId)
                .build();
        return FirebaseApp.initializeApp(options);
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
