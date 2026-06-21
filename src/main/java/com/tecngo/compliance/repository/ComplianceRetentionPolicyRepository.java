package com.tecngo.compliance.repository;

import com.tecngo.compliance.entity.ComplianceRetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplianceRetentionPolicyRepository extends JpaRepository<ComplianceRetentionPolicy, UUID> {
    List<ComplianceRetentionPolicy> findByActiveTrueOrderByDataCategory();
    Optional<ComplianceRetentionPolicy> findByDataCategory(String dataCategory);
}
