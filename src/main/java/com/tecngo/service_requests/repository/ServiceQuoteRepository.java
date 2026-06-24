package com.tecngo.service_requests.repository;

import com.tecngo.service_requests.entity.QuoteStatus;
import com.tecngo.service_requests.entity.ServiceQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceQuoteRepository extends JpaRepository<ServiceQuote, UUID> {
    Optional<ServiceQuote> findFirstByServiceRequestIdAndTechnicianIdAndStatus(
            UUID serviceRequestId, UUID technicianId, QuoteStatus status);
    List<ServiceQuote> findByServiceRequestIdInAndTechnicianIdAndStatus(
            List<UUID> serviceRequestIds, UUID technicianId, QuoteStatus status);
    @EntityGraph(attributePaths = {"technician", "serviceRequest"})
    List<ServiceQuote> findByServiceRequestIdOrderByCreatedAtAsc(UUID serviceRequestId);
    List<ServiceQuote> findByServiceRequestIdAndStatus(UUID serviceRequestId, QuoteStatus status);

    @Modifying
    @Query("""
            update ServiceQuote quote
            set quote.status = com.tecngo.service_requests.entity.QuoteStatus.EXPIRED,
                quote.respondedAt = :now
            where quote.status = com.tecngo.service_requests.entity.QuoteStatus.PENDING
              and quote.expiresAt <= :now
            """)
    int expirePending(@Param("now") Instant now);
}
