package com.tecngo.notifications.repository;

import com.tecngo.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<Notification> findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(
            UUID userId, java.time.Instant after, Pageable pageable);
    long countByUserIdAndReadFalse(UUID userId);
}
