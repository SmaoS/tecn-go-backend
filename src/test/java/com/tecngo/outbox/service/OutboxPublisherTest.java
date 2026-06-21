package com.tecngo.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecngo.outbox.entity.OutboxEvent;
import com.tecngo.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboxPublisherTest {
    private final OutboxEventRepository events = mock(OutboxEventRepository.class);
    private final OutboxPublisher publisher = new OutboxPublisher(events, new ObjectMapper());

    @Test
    void duplicateExternalEventReturnsExistingOutboxRecord() {
        OutboxEvent existing = OutboxEvent.builder().id(UUID.randomUUID())
                .externalKey("WOMPI-checksum").build();
        when(events.findByExternalKey("WOMPI-checksum")).thenReturn(Optional.of(existing));

        OutboxEvent result = publisher.publishExternal(
                OutboxPublisher.WOMPI_TRANSACTION_UPDATED,
                "TECHNICIAN_RECHARGE",
                "reference",
                "WOMPI-checksum",
                Map.of("status", "APPROVED"));

        assertThat(result).isSameAs(existing);
        verify(events, never()).save(any());
    }
}
