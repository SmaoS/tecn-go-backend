package com.tecngo.service_requests.repository;

import com.tecngo.service_requests.entity.ServiceRequestImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceRequestImageRepository extends JpaRepository<ServiceRequestImage, UUID> {
    List<ServiceRequestImage> findByServiceRequestIdOrderByCreatedAtAsc(UUID serviceRequestId);
    long countByServiceRequestId(UUID serviceRequestId);
}
