package com.tecngo.service_requests.repository;

import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.entity.RequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    List<ServiceRequest> findByClientIdOrderByCreatedAtDesc(UUID clientId);
    List<ServiceRequest> findByTechnicianIdOrderByCreatedAtDesc(UUID technicianId);
    List<ServiceRequest> findByClientIdAndStatusInOrderByCreatedAtDesc(UUID clientId, Set<RequestStatus> statuses);
    List<ServiceRequest> findByTechnicianIdAndStatusInOrderByCreatedAtDesc(UUID technicianId, Set<RequestStatus> statuses);
    @Query("""
            select distinct request from ServiceRequest request
            join request.category category
            where request.status = :status
              and request.latitude is not null
              and request.longitude is not null
              and category.id in :categoryIds
            order by request.createdAt asc
            """)
    List<ServiceRequest> findAvailable(@Param("status") RequestStatus status,
                                       @Param("categoryIds") List<UUID> categoryIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ServiceRequest request where request.id = :id")
    Optional<ServiceRequest> findByIdForUpdate(@Param("id") UUID id);
}
