package com.tecngo.payment_proofs.service;

import com.tecngo.files.service.FileStorage;
import com.tecngo.payment_proofs.dto.*;
import com.tecngo.payment_proofs.entity.*;
import com.tecngo.payment_proofs.repository.PaymentProofRepository;
import com.tecngo.payments.entity.PaymentStatus;
import com.tecngo.payments.repository.PaymentRepository;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.*;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.entity.*;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class PaymentProofService {
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");
    private final PaymentProofRepository proofs;
    private final ServiceRequestRepository requests;
    private final PaymentRepository payments;
    private final FileStorage storage;
    private final SystemParameterService parameters;
    private final ApplicationEventPublisher events;

    @Transactional
    public PaymentProofResponse upload(UUID requestId, BigDecimal amount, ProofPaymentMethod method,
                                       MultipartFile file, User user) {
        ServiceRequest request = requireRequest(requestId);
        requireParticipant(request, user);
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero");
        if (proofs.countByServiceRequestId(requestId) >= parameters.maxPaymentProofFiles())
            throw new ConflictException("Maximum number of payment proofs reached");
        var stored = storage.store(file, false, "tecngo/payment-proofs", TYPES);
        PaymentProof proof = proofs.save(PaymentProof.builder().serviceRequest(request).uploadedBy(user)
                .fileUrl(stored.accessUrl()).publicId(stored.publicId()).amount(amount)
                .paymentMethod(method).build());
        if (user.getRole() == Role.CLIENT && request.getTechnician() != null) {
            notifyUser(request.getTechnician(), request, "Nuevo comprobante de pago",
                    user.getFullName() + " subió un comprobante de pago",
                    NotificationType.PAYMENT_PROOF_UPLOADED);
        }
        return map(proof);
    }
    @Transactional(readOnly = true)
    public List<PaymentProofResponse> list(UUID requestId, User user) {
        ServiceRequest request = requireRequest(requestId);
        boolean staff = user.getRole() == Role.ADMIN || user.getRole() == Role.VERIFIER;
        if (!staff) requireParticipant(request, user);
        return proofs.findByServiceRequestIdOrderByCreatedAtDesc(requestId).stream().map(this::map).toList();
    }
    @Transactional(readOnly = true)
    public List<PaymentProofResponse> pending(User user) {
        requireReviewer(user);
        return proofs.findByStatusOrderByCreatedAtAsc(PaymentProofStatus.PENDING_REVIEW).stream().map(this::map).toList();
    }
    @Transactional
    public PaymentProofResponse review(UUID id, boolean approve, String comment, User reviewer) {
        requireReviewer(reviewer);
        PaymentProof proof = proofs.findById(id).orElseThrow(() -> new NotFoundException("Payment proof not found"));
        if (proof.getStatus() != PaymentProofStatus.PENDING_REVIEW)
            throw new ConflictException("Payment proof was already reviewed");
        if (!approve && (comment == null || comment.isBlank()))
            throw new IllegalArgumentException("Review comment is required when rejecting");
        if (approve && proofs.existsByServiceRequestIdAndStatus(
                proof.getServiceRequest().getId(), PaymentProofStatus.APPROVED))
            throw new ConflictException("An approved proof already exists for this service");
        proof.setStatus(approve ? PaymentProofStatus.APPROVED : PaymentProofStatus.REJECTED);
        proof.setReviewedBy(reviewer);
        proof.setReviewedAt(Instant.now());
        proof.setReviewComment(clean(comment));
        if (approve) payments.findByServiceRequestId(proof.getServiceRequest().getId())
                .ifPresent(payment -> payment.setStatus(PaymentStatus.PAID));
        ServiceRequest request = proof.getServiceRequest();
        notifyUser(request.getClient(), request,
                approve ? "Pago verificado" : "Comprobante rechazado",
                approve ? "El comprobante de pago fue aprobado"
                        : "El comprobante de pago fue rechazado: " + clean(comment),
                NotificationType.PAYMENT_PROOF_VERIFIED);
        return map(proof);
    }
    private ServiceRequest requireRequest(UUID id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Service request not found"));
    }
    private void requireParticipant(ServiceRequest request, User user) {
        boolean participant = request.getClient().getId().equals(user.getId())
                || request.getTechnician() != null && request.getTechnician().getId().equals(user.getId());
        if (!participant || user.getRole() != Role.CLIENT && user.getRole() != Role.TECHNICIAN)
            throw new ForbiddenException("Only service participants can manage payment proofs");
    }
    private void requireReviewer(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.VERIFIER)
            throw new ForbiddenException("Admin or verifier role is required");
    }
    private PaymentProofResponse map(PaymentProof item) {
        return new PaymentProofResponse(item.getId(), item.getServiceRequest().getId(),
                item.getUploadedBy().getId(), item.getUploadedBy().getFullName(), item.getFileUrl(),
                item.getAmount(), item.getPaymentMethod(), item.getStatus(),
                item.getReviewedBy() == null ? null : item.getReviewedBy().getId(), item.getReviewedAt(),
                item.getReviewComment(), item.getCreatedAt());
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void notifyUser(User recipient, ServiceRequest request, String title, String message,
                            NotificationType type) {
        events.publishEvent(new UserNotificationEvent(recipient.getId(), title, message, type,
                Map.of("type", "SERVICE_REQUEST", "requestId", request.getId().toString(),
                        "route", "ServiceSupport")));
    }
}
