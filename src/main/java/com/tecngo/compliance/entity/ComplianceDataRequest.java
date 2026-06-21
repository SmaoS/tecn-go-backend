package com.tecngo.compliance.entity;

import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_data_requests")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ComplianceDataRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private DataRequestType requestType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private DataRequestStatus status;
    @Column(length = 1000)
    private String reason;
    @Column(nullable = false)
    private Instant requestedAt;
    private Instant completedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @PrePersist
    void create() {
        if (requestedAt == null) requestedAt = Instant.now();
        if (status == null) status = DataRequestStatus.PENDING;
    }
}
