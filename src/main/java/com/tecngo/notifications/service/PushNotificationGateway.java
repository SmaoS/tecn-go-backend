package com.tecngo.notifications.service;

import java.util.Collection;
import java.util.Map;

public interface PushNotificationGateway {
    void send(String token, String title, String message, Map<String, String> data);
    void sendMany(Collection<String> tokens, String title, String message, Map<String, String> data);
}
