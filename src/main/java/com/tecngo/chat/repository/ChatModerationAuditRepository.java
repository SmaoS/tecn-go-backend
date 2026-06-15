package com.tecngo.chat.repository;

import com.tecngo.chat.entity.ChatModerationAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatModerationAuditRepository extends JpaRepository<ChatModerationAudit, UUID> {
}
