package com.tecngo.compliance.repository;

import com.tecngo.compliance.entity.ComplianceAccessAudit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceAccessAuditRepository extends JpaRepository<ComplianceAccessAudit, UUID> {
    List<ComplianceAccessAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<ComplianceAccessAudit> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);
}
