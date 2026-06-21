package com.tecngo.compliance.repository;

import com.tecngo.compliance.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceDataRequestRepository extends JpaRepository<ComplianceDataRequest, UUID> {
    List<ComplianceDataRequest> findByStatusOrderByRequestedAtAsc(DataRequestStatus status);
    List<ComplianceDataRequest> findByUserIdOrderByRequestedAtDesc(UUID userId);
    boolean existsByUserIdAndRequestTypeAndStatus(UUID userId, DataRequestType type, DataRequestStatus status);
}
