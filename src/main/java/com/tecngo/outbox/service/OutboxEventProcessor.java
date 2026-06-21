package com.tecngo.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.notifications.service.NotificationService;
import com.tecngo.outbox.entity.OutboxEvent;
import com.tecngo.technician_wallet.service.TechnicianWalletService;
import com.tecngo.wompi.dto.WompiTransactionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxEventProcessor {
    private final ObjectMapper objectMapper;
    private final NotificationService notifications;
    private final TechnicianWalletService wallets;

    public void process(OutboxEvent event) throws Exception {
        switch (event.getEventType()) {
            case OutboxPublisher.USER_NOTIFICATION -> notifications.deliverOutbox(
                    event.getId(),
                    objectMapper.readValue(event.getPayload(), UserNotificationEvent.class));
            case OutboxPublisher.WOMPI_TRANSACTION_UPDATED -> wallets.applyWompiTransaction(
                    objectMapper.readValue(event.getPayload(), WompiTransactionSnapshot.class));
            default -> throw new IllegalArgumentException(
                    "Unsupported outbox event type: " + event.getEventType());
        }
    }
}
