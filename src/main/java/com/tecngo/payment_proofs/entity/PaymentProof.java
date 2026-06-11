package com.tecngo.payment_proofs.entity;

import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "payment_proofs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentProof {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id") private ServiceRequest serviceRequest;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "uploaded_by_user_id") private User uploadedBy;
    @Column(nullable = false, length = 1000) private String fileUrl;
    @Column(nullable = false, length = 500) private String publicId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ProofPaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentProofStatus status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by_user_id") private User reviewedBy;
    private Instant reviewedAt;
    @Column(length = 1000) private String reviewComment;
    @Column(nullable = false) private Instant createdAt;
    @PrePersist void create() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = PaymentProofStatus.PENDING_REVIEW;
    }
}
