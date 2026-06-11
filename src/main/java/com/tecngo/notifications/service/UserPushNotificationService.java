package com.tecngo.notifications.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface UserPushNotificationService {
    void sendPush(UUID userId, String title, String message, Map<String, String> data);
    void sendPushToMany(Collection<UUID> userIds, String title, String message, Map<String, String> data);
}
