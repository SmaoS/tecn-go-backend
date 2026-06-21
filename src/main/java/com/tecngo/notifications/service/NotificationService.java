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
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final UserPushNotificationService pushNotifications;

    @Transactional(readOnly = true)
    public List<NotificationResponse> mine(User user, java.time.Instant after, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        var page = PageRequest.of(0, boundedLimit);
        var items = after == null
                ? notifications.findByUserIdOrderByCreatedAtDesc(user.getId(), page)
                : notifications.findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(user.getId(), after, page);
        return items.stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notifications.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markRead(UUID id, User user) {
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        ensureOwner(notification, user);
        notification.setRead(true);
        return map(notification);
    }

    @Transactional
    public void delete(UUID id, User user) {
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        ensureOwner(notification, user);
        notifications.delete(notification);
    }

    public void onNotification(UserNotificationEvent event) {
        deliverOutbox(null, event);
    }

    @Transactional
    public void persistOutbox(UUID outboxEventId, UserNotificationEvent event) {
        User user = users.findById(event.userId())
                .orElseThrow(() -> new NotFoundException("Notification user not found"));
        if (outboxEventId == null || notifications.findByOutboxEventId(outboxEventId).isEmpty()) {
            notifications.saveAndFlush(Notification.builder()
                    .user(user)
                    .title(event.title())
                    .message(event.message())
                    .type(event.type())
                    .route(event.data().get("route"))
                    .requestId(parseUuid(event.data().get("requestId")))
                    .outboxEventId(outboxEventId)
                    .build());
        }
    }

    @Transactional
    public void deliverOutbox(UUID outboxEventId, UserNotificationEvent event) {
        persistOutbox(outboxEventId, event);
        Map<String, String> pushData = new HashMap<>(event.data());
        pushData.put("notificationType", event.type().name());
        pushNotifications.sendPush(event.userId(), event.title(), event.message(), Map.copyOf(pushData));
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

    private void ensureOwner(Notification notification, User user) {
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Notification belongs to another user");
        }
    }
}
