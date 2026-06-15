package com.tecngo.chat.entity;

import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_moderation_audits")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatModerationAudit {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id")
    private ChatMessage message;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ChatAuditAction action;
    @Enumerated(EnumType.STRING)
    private ChatModerationStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ChatModerationStatus newStatus;
    @Column(length = 1000)
    private String reason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void create() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
