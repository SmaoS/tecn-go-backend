package com.tecngo.service_security.repository;

import com.tecngo.service_security.entity.ServiceSecurityAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceSecurityAuditRepository extends JpaRepository<ServiceSecurityAudit, UUID> {
}
