package com.tecngo.notifications.service;

import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FirebaseNotificationService implements UserPushNotificationService {
    private final UserRepository users;
    private final PushNotificationGateway gateway;

    @Override
    @Transactional(readOnly = true)
    public void sendPush(UUID userId, String title, String message, Map<String, String> data) {
        users.findById(userId)
                .map(user -> user.getFcmToken())
                .filter(token -> !token.isBlank())
                .ifPresent(token -> gateway.send(token, title, message, data));
    }

    @Override
    @Transactional(readOnly = true)
    public void sendPushToMany(Collection<UUID> userIds, String title, String message, Map<String, String> data) {
        var tokens = users.findAllById(userIds).stream()
                .map(user -> user.getFcmToken())
                .filter(token -> token != null && !token.isBlank())
                .toList();
        if (!tokens.isEmpty()) gateway.sendMany(tokens, title, message, data);
    }
}
