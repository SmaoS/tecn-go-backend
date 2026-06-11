package com.tecngo.service_requests.service;

import com.tecngo.files.service.FileStorage;
import com.tecngo.service_requests.dto.ServiceRequestImageResponse;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.entity.ServiceRequestImage;
import com.tecngo.service_requests.repository.ServiceRequestImageRepository;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRequestImageService {
    private static final Set<String> TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final ServiceRequestImageRepository images;
    private final ServiceRequestRepository requests;
    private final FileStorage storage;
    private final SystemParameterService parameters;

    @Transactional
    public ServiceRequestImageResponse upload(UUID requestId, MultipartFile file, User client) {
        ServiceRequest request = requireRequest(requestId);
        requireOwnerBeforeAcceptance(request, client);
        if (images.countByServiceRequestId(requestId) >= parameters.maxServiceRequestImages()) {
            throw new ConflictException("Maximum number of service images reached");
        }
        var stored = storage.store(file, true, "tecngo/service-requests", TYPES);
        return map(images.save(ServiceRequestImage.builder()
                .serviceRequest(request)
                .imageUrl(stored.secureUrl())
                .publicId(stored.publicId())
                .build()));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestImageResponse> list(UUID requestId, User user) {
        ServiceRequest request = requireRequest(requestId);
        boolean participant = request.getClient().getId().equals(user.getId())
                || request.getTechnician() != null && request.getTechnician().getId().equals(user.getId())
                || user.getRole() == Role.TECHNICIAN && request.getStatus() == RequestStatus.QUOTE_PENDING
                || user.getRole() == Role.ADMIN;
        if (!participant) throw new ForbiddenException("Service images are only visible to participants");
        return images.findByServiceRequestIdOrderByCreatedAtAsc(requestId).stream().map(this::map).toList();
    }

    @Transactional
    public void delete(UUID requestId, UUID imageId, User client) {
        ServiceRequest request = requireRequest(requestId);
        requireOwnerBeforeAcceptance(request, client);
        ServiceRequestImage image = images.findById(imageId)
                .filter(item -> item.getServiceRequest().getId().equals(requestId))
                .orElseThrow(() -> new NotFoundException("Service image not found"));
        images.delete(image);
        storage.delete(image.getPublicId());
    }

    private void requireOwnerBeforeAcceptance(ServiceRequest request, User client) {
        if (client.getRole() != Role.CLIENT || !request.getClient().getId().equals(client.getId())) {
            throw new ForbiddenException("Only the client owner can modify service images");
        }
        if (request.getStatus() != RequestStatus.QUOTE_PENDING) {
            throw new ConflictException("Service images cannot be changed after accepting a quote");
        }
    }

    private ServiceRequest requireRequest(UUID id) {
        return requests.findById(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
    }

    private ServiceRequestImageResponse map(ServiceRequestImage item) {
        return new ServiceRequestImageResponse(item.getId(), item.getServiceRequest().getId(),
                item.getImageUrl(), item.getPublicId(), item.getCreatedAt());
    }
}
