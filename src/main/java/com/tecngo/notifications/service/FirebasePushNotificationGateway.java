package com.tecngo.notifications.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.BatchResponse;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class FirebasePushNotificationGateway implements PushNotificationGateway {
    private static final int MAX_MULTICAST_TOKENS = 500;
    private final FirebaseMessaging messaging;

    @Override
    public void send(String token, String title, String message, Map<String, String> data) {
        Message push = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(title).setBody(message).build())
                .putAllData(data)
                .build();
        ApiFutures.addCallback(messaging.sendAsync(push), new ApiFutureCallback<>() {
            @Override
            public void onFailure(Throwable error) {
                log.warn("FCM push failed for token ending in {}: {}", suffix(token), error.getMessage());
            }

            @Override
            public void onSuccess(String messageId) {
                log.debug("FCM push sent for token ending in {}: {}", suffix(token), messageId);
            }
        }, Runnable::run);
    }

    @Override
    public void sendMany(Collection<String> tokens, String title, String message, Map<String, String> data) {
        List<String> uniqueTokens = tokens.stream().filter(token -> token != null && !token.isBlank()).distinct().toList();
        for (int start = 0; start < uniqueTokens.size(); start += MAX_MULTICAST_TOKENS) {
            List<String> batch = new ArrayList<>(uniqueTokens.subList(start,
                    Math.min(start + MAX_MULTICAST_TOKENS, uniqueTokens.size())));
            MulticastMessage push = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .setNotification(Notification.builder().setTitle(title).setBody(message).build())
                    .putAllData(data)
                    .build();
            ApiFutures.addCallback(messaging.sendEachForMulticastAsync(push), new ApiFutureCallback<>() {
                @Override
                public void onFailure(Throwable error) {
                    log.warn("FCM multicast failed for {} devices: {}", batch.size(), error.getMessage());
                }

                @Override
                public void onSuccess(BatchResponse response) {
                    log.info("FCM multicast sent: {} successful, {} failed",
                            response.getSuccessCount(), response.getFailureCount());
                }
            }, Runnable::run);
        }
    }

    private String suffix(String token) {
        return token.length() <= 8 ? token : token.substring(token.length() - 8);
    }
}
