package com.tecngo.outbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(length = 80)
    private String aggregateType;

    @Column(length = 120)
    private String aggregateId;

    @Column(length = 255, unique = true)
    private String externalKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant availableAt;

    private Instant lockedAt;
    private Instant processedAt;

    @Column(length = 2000)
    private String lastError;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (status == null) status = OutboxStatus.PENDING;
        if (availableAt == null) availableAt = now;
        if (createdAt == null) createdAt = now;
    }
}
