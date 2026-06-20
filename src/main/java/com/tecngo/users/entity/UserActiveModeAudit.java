package com.tecngo.users.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_active_mode_audits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActiveModeAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedBy;

    @Enumerated(EnumType.STRING)
    private ActiveMode previousMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActiveMode newMode;

    @Column(nullable = false, length = 120)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
