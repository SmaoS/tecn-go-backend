package com.tecngo.notifications.service;

import com.tecngo.notifications.dto.NotificationResponse;
import com.tecngo.notifications.entity.Notification;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.notifications.repository.NotificationRepository;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final UserPushNotificationService pushNotifications;

    @Transactional(readOnly = true)
    public List<NotificationResponse> mine(User user) {
        return notifications.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notifications.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markRead(UUID id, User user) {
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Notification belongs to another user");
        }
        notification.setRead(true);
        return map(notification);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNotification(UserNotificationEvent event) {
        User user = users.findById(event.userId())
                .orElseThrow(() -> new NotFoundException("Notification user not found"));
        notifications.save(Notification.builder()
                .user(user)
                .title(event.title())
                .message(event.message())
                .type(event.type())
                .route(event.data().get("route"))
                .requestId(parseUuid(event.data().get("requestId")))
                .build());
        pushNotifications.sendPush(user.getId(), event.title(), event.message(), event.data());
    }

    private NotificationResponse map(Notification item) {
        return new NotificationResponse(item.getId(), item.getTitle(), item.getMessage(),
                item.getType(), item.isRead(), item.getCreatedAt(), item.getRoute(), item.getRequestId());
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
