package com.tecngo.technicians.repository;

import com.tecngo.technicians.entity.TechnicianProfile;
import com.tecngo.technicians.entity.TechnicianStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface TechnicianProfileRepository extends JpaRepository<TechnicianProfile, UUID> {
    Optional<TechnicianProfile> findByUserId(UUID userId);
    List<TechnicianProfile> findByStatusOrderByCreatedAtAsc(TechnicianStatus status);
    long countByStatus(TechnicianStatus status);
    boolean existsByUserId(UUID userId);
    boolean existsByDocumentNumber(String documentNumber);

    @Query("""
            select distinct profile from TechnicianProfile profile
            join fetch profile.user
            left join fetch profile.categories
            where profile.user.id in :userIds
            """)
    List<TechnicianProfile> findWithCategoriesByUserIdIn(@Param("userIds") Collection<UUID> userIds);
}
