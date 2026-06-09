package com.tecngo.technicians.repository;

import com.tecngo.technicians.entity.TechnicianProfile;
import com.tecngo.technicians.entity.TechnicianStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TechnicianProfileRepository extends JpaRepository<TechnicianProfile, UUID> {
    Optional<TechnicianProfile> findByUserId(UUID userId);
    List<TechnicianProfile> findByStatusOrderByCreatedAtAsc(TechnicianStatus status);
    boolean existsByUserId(UUID userId);
    boolean existsByDocumentNumber(String documentNumber);
}
