package com.tecngo.chat.entity;

import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_message_reports")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatMessageReport {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id")
    private ChatMessage message;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_id")
    private User reportedBy;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ChatReportStatus status;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant resolvedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    @PrePersist
    void create() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = ChatReportStatus.OPEN;
    }
}
