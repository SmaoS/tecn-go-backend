package com.tecngo.payment_proofs.repository;
import com.tecngo.payment_proofs.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PaymentProofRepository extends JpaRepository<PaymentProof, UUID> {
    long countByServiceRequestId(UUID requestId);
    boolean existsByServiceRequestIdAndStatus(UUID requestId, PaymentProofStatus status);
    List<PaymentProof> findByServiceRequestIdOrderByCreatedAtDesc(UUID requestId);
    List<PaymentProof> findByStatusOrderByCreatedAtAsc(PaymentProofStatus status);
    Optional<PaymentProof> findByFileUrl(String fileUrl);
    Optional<PaymentProof> findByContentAssetId(UUID contentAssetId);
}
