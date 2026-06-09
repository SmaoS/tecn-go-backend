package com.tecngo.notifications.event;

import com.tecngo.notifications.entity.NotificationType;

import java.util.UUID;

public record UserNotificationEvent(
        UUID userId, String title, String message, NotificationType type
) {}
