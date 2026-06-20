package com.tecngo.users.repository;

import com.tecngo.users.entity.UserActiveModeAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserActiveModeAuditRepository extends JpaRepository<UserActiveModeAudit, UUID> {
    List<UserActiveModeAudit> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
