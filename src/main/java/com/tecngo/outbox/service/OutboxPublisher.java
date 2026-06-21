package com.tecngo.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecngo.outbox.entity.OutboxEvent;
import com.tecngo.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {
    public static final String USER_NOTIFICATION = "USER_NOTIFICATION";
    public static final String WOMPI_TRANSACTION_UPDATED = "WOMPI_TRANSACTION_UPDATED";

    private final OutboxEventRepository events;
    private final ObjectMapper objectMapper;

    @Transactional
    public OutboxEvent publish(String eventType, String aggregateType,
                               String aggregateId, Object payload) {
        return save(eventType, aggregateType, aggregateId, null, payload);
    }

    @Transactional
    public OutboxEvent publishExternal(String eventType, String aggregateType,
                                       String aggregateId, String externalKey, Object payload) {
        return events.findByExternalKey(externalKey)
                .orElseGet(() -> save(eventType, aggregateType, aggregateId, externalKey, payload));
    }

    private OutboxEvent save(String eventType, String aggregateType, String aggregateId,
                             String externalKey, Object payload) {
        try {
            return events.save(OutboxEvent.builder()
                    .eventType(eventType)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .externalKey(externalKey)
                    .payload(objectMapper.writeValueAsString(payload))
                    .build());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize outbox event", exception);
        }
    }
}
