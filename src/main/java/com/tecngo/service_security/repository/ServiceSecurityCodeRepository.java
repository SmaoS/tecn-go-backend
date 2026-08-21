package com.tecngo.service_security.repository;

import com.tecngo.service_security.entity.ServiceSecurityCode;
import com.tecngo.service_security.entity.ServiceSecurityCodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface ServiceSecurityCodeRepository extends JpaRepository<ServiceSecurityCode, UUID> {
    Optional<ServiceSecurityCode> findFirstByServiceRequestIdAndStatusOrderByCreatedAtDesc(
            UUID serviceRequestId, ServiceSecurityCodeStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ServiceSecurityCode> findFirstByServiceRequestIdAndStatus(
            UUID serviceRequestId, ServiceSecurityCodeStatus status);

    Optional<ServiceSecurityCode> findFirstByServiceRequestIdOrderByCreatedAtDesc(UUID serviceRequestId);
}
