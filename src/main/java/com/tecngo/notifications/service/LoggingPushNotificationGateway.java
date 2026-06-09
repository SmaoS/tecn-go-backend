package com.tecngo.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingPushNotificationGateway implements PushNotificationGateway {
    @Override
    public void send(String token, String title, String message) {
        log.info("FCM push prepared for token ending in {}: {}", suffix(token), title);
    }

    private String suffix(String token) {
        return token.length() <= 8 ? token : token.substring(token.length() - 8);
    }
}
