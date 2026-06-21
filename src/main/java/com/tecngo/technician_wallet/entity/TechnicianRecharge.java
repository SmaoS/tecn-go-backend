package com.tecngo.technician_wallet.entity;

import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "technician_recharges")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianRecharge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id", nullable = false)
    private User technician;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, unique = true, length = 120)
    private String reference;

    @Column(length = 120, unique = true)
    private String wompiTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RechargeStatus status;

    @Column(nullable = false, length = 1200)
    private String paymentUrl;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant approvedAt;
    private Instant rejectedAt;
    @Column(nullable = false)
    private int reconciliationAttempts;
    private Instant nextReconciliationAt;
    private Instant lastReconciledAt;
    @Column(length = 1000)
    private String lastReconciliationError;

    @PrePersist
    void onCreate() {
        if (currency == null) currency = "COP";
        if (status == null) status = RechargeStatus.PENDING;
        if (createdAt == null) createdAt = Instant.now();
    }
}
