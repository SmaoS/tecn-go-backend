package com.tecngo.notifications.event;

import com.tecngo.notifications.entity.NotificationType;

import java.util.Map;
import java.util.UUID;

public record UserNotificationEvent(
        UUID userId, String title, String message, NotificationType type, Map<String, String> data
) {
    public UserNotificationEvent(UUID userId, String title, String message, NotificationType type) {
        this(userId, title, message, type, Map.of());
    }
}
