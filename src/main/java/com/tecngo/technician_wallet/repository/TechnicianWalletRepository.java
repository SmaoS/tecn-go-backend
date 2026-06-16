package com.tecngo.technician_wallet.repository;

import com.tecngo.technician_wallet.entity.TechnicianWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TechnicianWalletRepository extends JpaRepository<TechnicianWallet, UUID> {
    Optional<TechnicianWallet> findByTechnicianId(UUID technicianId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from TechnicianWallet w where w.technician.id = :technicianId")
    Optional<TechnicianWallet> findByTechnicianIdForUpdate(@Param("technicianId") UUID technicianId);

    List<TechnicianWallet> findAllByOrderByUpdatedAtDesc();
}
