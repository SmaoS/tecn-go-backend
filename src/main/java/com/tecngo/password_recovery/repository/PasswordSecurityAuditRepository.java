package com.tecngo.password_recovery.repository;

import com.tecngo.password_recovery.entity.PasswordSecurityAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PasswordSecurityAuditRepository extends JpaRepository<PasswordSecurityAudit, UUID> {
}
