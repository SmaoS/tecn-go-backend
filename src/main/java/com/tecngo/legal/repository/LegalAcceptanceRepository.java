package com.tecngo.legal.repository;
import com.tecngo.legal.entity.LegalAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LegalAcceptanceRepository extends JpaRepository<LegalAcceptance, UUID> {
    boolean existsByUserIdAndLegalDocumentId(UUID userId, UUID documentId);
    List<LegalAcceptance> findByUserId(UUID userId);
}
