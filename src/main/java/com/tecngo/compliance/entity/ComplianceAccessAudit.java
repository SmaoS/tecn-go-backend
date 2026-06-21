package com.tecngo.compliance.entity;

import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_access_audits")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ComplianceAccessAudit {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_user_id")
    private User subject;
    @Column(nullable = false, length = 80)
    private String resourceType;
    @Column(length = 255)
    private String resourceId;
    @Column(nullable = false, length = 120)
    private String action;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AuditOutcome outcome;
    @Column(length = 100)
    private String correlationId;
    @Column(length = 64)
    private String ipHash;
    @Column(length = 1000)
    private String details;
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void create() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
