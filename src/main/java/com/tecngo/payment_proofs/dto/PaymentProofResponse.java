package com.tecngo.payment_proofs.dto;
import com.tecngo.payment_proofs.entity.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record PaymentProofResponse(UUID id, UUID serviceRequestId, UUID uploadedByUserId,
        String uploadedByName, String fileUrl, BigDecimal amount, ProofPaymentMethod paymentMethod,
        PaymentProofStatus status, UUID reviewedByUserId, Instant reviewedAt,
        String reviewComment, Instant createdAt) {}
