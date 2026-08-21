package com.tecngo.service_security.entity;

import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_security_audits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSecurityAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ServiceRequest serviceRequest;
    @ManyToOne(fetch = FetchType.LAZY)
    private User technician;
    @ManyToOne(fetch = FetchType.LAZY)
    private User client;
    @ManyToOne(fetch = FetchType.LAZY)
    private User actor;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceSecurityAuditAction action;
    @Column(nullable = false)
    private int attempts;
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void defaults() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
