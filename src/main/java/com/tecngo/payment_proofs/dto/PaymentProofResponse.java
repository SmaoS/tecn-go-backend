package com.tecngo.payment_proofs.dto;
import com.tecngo.payment_proofs.entity.*;
import com.tecngo.content_moderation.entity.ModerationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record PaymentProofResponse(UUID id, UUID serviceRequestId, UUID uploadedByUserId,
        String uploadedByName, String fileUrl, BigDecimal amount, ProofPaymentMethod paymentMethod,
        PaymentProofStatus status, UUID reviewedByUserId, Instant reviewedAt,
        String reviewComment, UUID contentAssetId, ModerationStatus moderationStatus, Instant createdAt) {}
