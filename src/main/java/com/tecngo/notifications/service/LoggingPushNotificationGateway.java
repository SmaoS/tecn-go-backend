package com.tecngo.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class LoggingPushNotificationGateway implements PushNotificationGateway {
    @Override
    public void send(String token, String title, String message, Map<String, String> data) {
        log.info("FCM push prepared for token ending in {}: {}", suffix(token), title);
    }

    @Override
    public void sendMany(Collection<String> tokens, String title, String message, Map<String, String> data) {
        log.info("FCM multicast prepared for {} tokens: {}", tokens.size(), title);
    }

    private String suffix(String token) {
        return token.length() <= 8 ? token : token.substring(token.length() - 8);
    }
}
