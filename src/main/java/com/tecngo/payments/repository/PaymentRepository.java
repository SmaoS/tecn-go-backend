package com.tecngo.payments.repository;

import com.tecngo.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    boolean existsByServiceRequestId(UUID serviceRequestId);
    Optional<Payment> findByServiceRequestId(UUID serviceRequestId);
    List<Payment> findByClientIdOrderByCreatedAtDesc(UUID clientId);
    List<Payment> findByTechnicianIdOrderByCreatedAtDesc(UUID technicianId);
    List<Payment> findAllByOrderByCreatedAtDesc();
}
