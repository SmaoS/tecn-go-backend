package com.tecngo.technician_wallet.repository;

import com.tecngo.technician_wallet.entity.TechnicianRecharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Pageable;

public interface TechnicianRechargeRepository extends JpaRepository<TechnicianRecharge, UUID> {
    Optional<TechnicianRecharge> findByReference(String reference);
    Optional<TechnicianRecharge> findByWompiTransactionId(String wompiTransactionId);
    List<TechnicianRecharge> findByTechnicianIdOrderByCreatedAtDesc(UUID technicianId);
    Optional<TechnicianRecharge> findByIdAndTechnicianId(UUID id, UUID technicianId);

    @Query("""
            select recharge from TechnicianRecharge recharge
            where recharge.status = com.tecngo.technician_wallet.entity.RechargeStatus.PENDING
              and recharge.wompiTransactionId is not null
              and (recharge.nextReconciliationAt is null or recharge.nextReconciliationAt <= :now)
            order by recharge.createdAt
            """)
    List<TechnicianRecharge> findReconciliationCandidates(@Param("now") Instant now,
                                                           Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TechnicianRecharge r where r.reference = :reference")
    Optional<TechnicianRecharge> findByReferenceForUpdate(@Param("reference") String reference);
}
