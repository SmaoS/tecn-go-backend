package com.tecngo.technician_location.repository;

import com.tecngo.technician_location.entity.TechnicianLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TechnicianLocationRepository extends JpaRepository<TechnicianLocation, UUID> {
    Optional<TechnicianLocation> findByTechnicianId(UUID technicianId);
}
