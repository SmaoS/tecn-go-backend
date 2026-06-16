package com.tecngo.notifications.service;

import com.tecngo.notifications.entity.Notification;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.repository.NotificationRepository;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final UserPushNotificationService pushNotifications = mock(UserPushNotificationService.class);
    private final NotificationService service = new NotificationService(notifications, users, pushNotifications);

    @Test
    void deletesOwnNotification() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        Notification notification = Notification.builder()
                .id(notificationId)
                .user(user)
                .title("Nueva cotización")
                .message("Tienes una cotización")
                .type(NotificationType.NEW_QUOTE)
                .build();
        when(notifications.findById(notificationId)).thenReturn(Optional.of(notification));

        service.delete(notificationId, user);

        verify(notifications).delete(notification);
    }

    @Test
    void blocksDeletingAnotherUsersNotification() {
        UUID notificationId = UUID.randomUUID();
        User owner = User.builder().id(UUID.randomUUID()).build();
        User requester = User.builder().id(UUID.randomUUID()).build();
        Notification notification = Notification.builder()
                .id(notificationId)
                .user(owner)
                .title("Nueva cotización")
                .message("Tienes una cotización")
                .type(NotificationType.NEW_QUOTE)
                .build();
        when(notifications.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.delete(notificationId, requester))
                .isInstanceOf(ForbiddenException.class);
        verify(notifications, never()).delete(any());
    }
}
