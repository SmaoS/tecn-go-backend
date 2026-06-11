package com.tecngo.service_requests.repository;

import com.tecngo.service_requests.entity.QuoteStatus;
import com.tecngo.service_requests.entity.ServiceQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceQuoteRepository extends JpaRepository<ServiceQuote, UUID> {
    Optional<ServiceQuote> findByServiceRequestIdAndTechnicianId(UUID serviceRequestId, UUID technicianId);
    List<ServiceQuote> findByServiceRequestIdOrderByCreatedAtAsc(UUID serviceRequestId);
    List<ServiceQuote> findByServiceRequestIdAndStatus(UUID serviceRequestId, QuoteStatus status);
}
