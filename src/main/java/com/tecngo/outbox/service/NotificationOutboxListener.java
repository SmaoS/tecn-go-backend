package com.tecngo.outbox.service;

import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationOutboxListener {
    private final OutboxPublisher outbox;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void capture(UserNotificationEvent event) {
        var outboxEvent = outbox.publish(OutboxPublisher.USER_NOTIFICATION, "USER",
                event.userId().toString(), event);
        notifications.persistOutbox(outboxEvent.getId(), event);
    }
}
