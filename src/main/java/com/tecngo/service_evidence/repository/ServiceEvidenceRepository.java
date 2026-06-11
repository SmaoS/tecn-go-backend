package com.tecngo.service_evidence.repository;

import com.tecngo.service_evidence.entity.ServiceEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceEvidenceRepository extends JpaRepository<ServiceEvidence, UUID> {
    long countByServiceRequestId(UUID requestId);
    List<ServiceEvidence> findByServiceRequestIdOrderByCreatedAtAsc(UUID requestId);
    Optional<ServiceEvidence> findByFileUrl(String fileUrl);
    List<ServiceEvidence> findAllByOrderByCreatedAtDesc();
}
