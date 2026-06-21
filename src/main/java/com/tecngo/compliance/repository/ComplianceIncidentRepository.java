package com.tecngo.compliance.repository;

import com.tecngo.compliance.entity.ComplianceIncident;
import com.tecngo.compliance.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceIncidentRepository extends JpaRepository<ComplianceIncident, UUID> {
    List<ComplianceIncident> findAllByOrderByDetectedAtDesc();
    List<ComplianceIncident> findByStatusOrderByDetectedAtDesc(IncidentStatus status);
}
