package com.tecngo.technician_wallet.repository;

import com.tecngo.technician_wallet.entity.TechnicianWalletTransaction;
import com.tecngo.technician_wallet.entity.WalletTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TechnicianWalletTransactionRepository extends JpaRepository<TechnicianWalletTransaction, UUID> {
    List<TechnicianWalletTransaction> findByTechnicianIdOrderByCreatedAtDesc(UUID technicianId);
    boolean existsByReferenceAndType(String reference, WalletTransactionType type);
}
