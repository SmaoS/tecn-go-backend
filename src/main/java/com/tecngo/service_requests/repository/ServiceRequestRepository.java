package com.tecngo.service_requests.repository;

import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.entity.RequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.time.Instant;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    List<ServiceRequest> findByClientIdOrderByCreatedAtDesc(UUID clientId);
    List<ServiceRequest> findByTechnicianIdOrderByCreatedAtDesc(UUID technicianId);
    List<ServiceRequest> findByClientIdAndStatusInOrderByCreatedAtDesc(UUID clientId, Set<RequestStatus> statuses);
    List<ServiceRequest> findByTechnicianIdAndStatusInOrderByCreatedAtDesc(UUID technicianId, Set<RequestStatus> statuses);

    @EntityGraph(attributePaths = {"client", "technician", "category", "city"})
    Page<ServiceRequest> findPageByClientId(UUID clientId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "technician", "category", "city"})
    Page<ServiceRequest> findPageByClientIdAndStatusIn(UUID clientId, Set<RequestStatus> statuses,
                                                       Pageable pageable);

    @EntityGraph(attributePaths = {"client", "technician", "category", "city"})
    Page<ServiceRequest> findPageByTechnicianId(UUID technicianId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "technician", "category", "city"})
    Page<ServiceRequest> findPageByTechnicianIdAndStatusIn(UUID technicianId, Set<RequestStatus> statuses,
                                                           Pageable pageable);
    @Query("""
            select distinct request from ServiceRequest request
            join fetch request.client
            left join fetch request.technician
            join fetch request.category category
            left join fetch request.city
            where request.status = :status
              and request.city.id = :cityId
              and category.id in :categoryIds
              and (:categoryId is null or category.id = :categoryId)
            order by request.createdAt desc
            """)
    List<ServiceRequest> findAvailable(@Param("status") RequestStatus status,
                                       @Param("cityId") UUID cityId,
                                       @Param("categoryIds") List<UUID> categoryIds,
                                       @Param("categoryId") UUID categoryId,
                                       Pageable pageable);

    @Query("""
            select distinct request from ServiceRequest request
            join fetch request.client
            left join fetch request.technician
            join fetch request.category category
            left join fetch request.city
            where request.status = :status
              and category.id in :categoryIds
              and (:categoryId is null or category.id = :categoryId)
            order by request.createdAt desc
            """)
    List<ServiceRequest> findAvailableWithoutCity(@Param("status") RequestStatus status,
                                                  @Param("categoryIds") List<UUID> categoryIds,
                                                  @Param("categoryId") UUID categoryId,
                                                  Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ServiceRequest request where request.id = :id")
    Optional<ServiceRequest> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"client", "category"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ServiceRequest> findByStatusAndExpiresAtLessThanEqual(
            RequestStatus status, Instant expiresAt);
}
