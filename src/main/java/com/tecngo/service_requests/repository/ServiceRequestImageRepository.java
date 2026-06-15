package com.tecngo.service_requests.repository;

import com.tecngo.service_requests.entity.ServiceRequestImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ServiceRequestImageRepository extends JpaRepository<ServiceRequestImage, UUID> {
    List<ServiceRequestImage> findByServiceRequestIdOrderByCreatedAtAsc(UUID serviceRequestId);
    long countByServiceRequestId(UUID serviceRequestId);
    Optional<ServiceRequestImage> findByContentAssetId(UUID contentAssetId);
    Optional<ServiceRequestImage> findByImageUrl(String imageUrl);
}
