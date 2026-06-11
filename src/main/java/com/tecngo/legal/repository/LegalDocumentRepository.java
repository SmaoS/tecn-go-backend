package com.tecngo.legal.repository;
import com.tecngo.legal.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.util.UUID;
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {
    List<LegalDocument> findByActiveTrueOrderByCodeAsc();
    List<LegalDocument> findAllByOrderByCodeAscCreatedAtDesc();
    Optional<LegalDocument> findByCodeAndRoleTargetAndActiveTrue(String code, LegalRoleTarget roleTarget);
}
