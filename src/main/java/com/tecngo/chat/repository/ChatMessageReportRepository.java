package com.tecngo.chat.repository;

import com.tecngo.chat.entity.ChatMessageReport;
import com.tecngo.chat.entity.ChatReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface ChatMessageReportRepository extends JpaRepository<ChatMessageReport, UUID> {
    boolean existsByMessageIdAndReportedByIdAndStatus(UUID messageId, UUID userId, ChatReportStatus status);
    long countByMessageIdAndStatus(UUID messageId, ChatReportStatus status);
    List<ChatMessageReport> findByMessageIdAndStatus(UUID messageId, ChatReportStatus status);
}
