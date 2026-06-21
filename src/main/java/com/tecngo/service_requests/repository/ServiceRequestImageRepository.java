package com.tecngo.service_requests.repository;

import com.tecngo.service_requests.entity.ServiceRequestImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ServiceRequestImageRepository extends JpaRepository<ServiceRequestImage, UUID> {
    List<ServiceRequestImage> findByServiceRequestIdOrderByCreatedAtAsc(UUID serviceRequestId);
    @EntityGraph(attributePaths = "contentAsset")
    List<ServiceRequestImage> findByServiceRequestIdInOrderByCreatedAtAsc(List<UUID> serviceRequestIds);
    long countByServiceRequestId(UUID serviceRequestId);
    Optional<ServiceRequestImage> findByContentAssetId(UUID contentAssetId);
    Optional<ServiceRequestImage> findByImageUrl(String imageUrl);
}
