package com.tecngo.compliance.entity;

import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_retention_policies")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ComplianceRetentionPolicy {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 60)
    private String dataCategory;
    @Column(nullable = false)
    private int retentionDays;
    @Column(nullable = false, length = 500)
    private String legalBasis;
    @Column(nullable = false)
    private boolean automaticDeletion;
    @Column(nullable = false)
    private boolean active;
    @Column(nullable = false)
    private Instant updatedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;

    @PrePersist @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
