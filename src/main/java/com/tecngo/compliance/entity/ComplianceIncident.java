package com.tecngo.compliance.entity;

import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_incidents")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ComplianceIncident {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(nullable = false, length = 4000)
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private IncidentSeverity severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private IncidentStatus status;
    @Column(nullable = false)
    private Instant detectedAt;
    private Instant containedAt;
    private Instant resolvedAt;
    @Column(nullable = false)
    private Instant createdAt;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_id")
    private User reportedBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;
    @Column(length = 4000)
    private String resolutionSummary;

    @PrePersist
    void create() {
        if (createdAt == null) createdAt = Instant.now();
        if (detectedAt == null) detectedAt = Instant.now();
        if (status == null) status = IncidentStatus.OPEN;
    }
}
