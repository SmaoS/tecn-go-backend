package com.tecngo.service_security.entity;

import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_security_codes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSecurityCode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private ServiceRequest serviceRequest;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;
    @Column(nullable = false, length = 10)
    private String codePlain;
    @Column(nullable = false, length = 128)
    private String codeHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceSecurityCodeStatus status;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant verifiedAt;
    @Column(nullable = false)
    private int verificationAttempts;
    @Column(nullable = false)
    private int maxAttempts;
    @Column(nullable = false)
    private int regeneratedCount;

    @PrePersist
    void defaults() {
        if (status == null) status = ServiceSecurityCodeStatus.ACTIVE;
        if (createdAt == null) createdAt = Instant.now();
    }
}
