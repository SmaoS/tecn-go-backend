package com.tecngo.notifications.service;

import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class FirebaseNotificationServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PushNotificationGateway gateway = mock(PushNotificationGateway.class);
    private final FirebaseNotificationService service = new FirebaseNotificationService(users, gateway);

    @Test
    void sendsPushToStoredTokenWithData() {
        UUID userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.of(
                User.builder().id(userId).fcmToken("device-token").build()));

        service.sendPush(userId, "Title", "Message", Map.of("requestId", "123"));

        verify(gateway).send("device-token", "Title", "Message", Map.of("requestId", "123"));
    }

    @Test
    void sendsMulticastOnlyToUsersWithTokens() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(users.findAllById(List.of(first, second))).thenReturn(List.of(
                User.builder().id(first).fcmToken("token-1").build(),
                User.builder().id(second).build()));

        service.sendPushToMany(List.of(first, second), "Title", "Message", Map.of());

        verify(gateway).sendMany(List.of("token-1"), "Title", "Message", Map.of());
    }
}
