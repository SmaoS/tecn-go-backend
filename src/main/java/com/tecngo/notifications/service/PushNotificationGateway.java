package com.tecngo.notifications.service;

public interface PushNotificationGateway {
    void send(String token, String title, String message);
}
