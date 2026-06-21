package com.tecngo.outbox.repository;

import com.tecngo.outbox.entity.OutboxEvent;
import com.tecngo.outbox.entity.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    Optional<OutboxEvent> findByExternalKey(String externalKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event from OutboxEvent event
            where (event.status = com.tecngo.outbox.entity.OutboxStatus.PENDING
                   and event.availableAt <= :now)
               or (event.status = com.tecngo.outbox.entity.OutboxStatus.PROCESSING
                   and event.lockedAt < :staleBefore)
            order by event.createdAt
            """)
    List<OutboxEvent> findDispatchable(@Param("now") Instant now,
                                       @Param("staleBefore") Instant staleBefore,
                                       Pageable pageable);

    long countByStatus(OutboxStatus status);
    List<OutboxEvent> findByStatusOrderByCreatedAtDesc(OutboxStatus status, Pageable pageable);
}
