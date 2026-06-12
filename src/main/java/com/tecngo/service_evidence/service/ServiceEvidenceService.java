package com.tecngo.service_evidence.service;

import com.tecngo.files.service.FileStorage;
import com.tecngo.service_evidence.dto.ServiceEvidenceResponse;
import com.tecngo.service_evidence.entity.*;
import com.tecngo.service_evidence.repository.ServiceEvidenceRepository;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.*;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.entity.*;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service @RequiredArgsConstructor
public class ServiceEvidenceService {
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");
    private final ServiceEvidenceRepository evidences;
    private final ServiceRequestRepository requests;
    private final FileStorage storage;
    private final SystemParameterService parameters;
    private final ApplicationEventPublisher events;

    @Transactional
    public ServiceEvidenceResponse upload(UUID requestId, EvidenceType type, String description,
                                          MultipartFile file, User user) {
        ServiceRequest request = requireRequest(requestId);
        requireParticipant(request, user, false);
        if (evidences.countByServiceRequestId(requestId) >= parameters.maxServiceEvidenceFiles())
            throw new ConflictException("Maximum number of service evidences reached");
        var stored = storage.store(file, false, "tecngo/service-evidences", TYPES);
        ServiceEvidence evidence = evidences.save(ServiceEvidence.builder().serviceRequest(request).uploadedBy(user)
                .uploadedByRole(user.getRole()).evidenceType(type).fileUrl(stored.accessUrl())
                .publicId(stored.publicId()).description(clean(description)).build());
        if (user.getRole() == Role.TECHNICIAN) {
            events.publishEvent(new UserNotificationEvent(
                    request.getClient().getId(),
                    "Nueva evidencia del servicio",
                    user.getFullName() + " subió una evidencia",
                    NotificationType.SERVICE_EVIDENCE_UPLOADED,
                    Map.of("type", "SERVICE_REQUEST", "requestId", request.getId().toString(),
                            "route", "ServiceSupport")));
        }
        return map(evidence);
    }
    @Transactional(readOnly = true)
    public List<ServiceEvidenceResponse> list(UUID requestId, User user) {
        ServiceRequest request = requireRequest(requestId);
        requireParticipant(request, user, true);
        return evidences.findByServiceRequestIdOrderByCreatedAtAsc(requestId).stream().map(this::map).toList();
    }
    @Transactional(readOnly = true)
    public List<ServiceEvidenceResponse> listAll(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.VERIFIER)
            throw new ForbiddenException("Admin or verifier role is required");
        return evidences.findAllByOrderByCreatedAtDesc().stream().map(this::map).toList();
    }
    @Transactional
    public void delete(UUID requestId, UUID evidenceId, User user) {
        ServiceRequest request = requireRequest(requestId);
        requireParticipant(request, user, true);
        ServiceEvidence evidence = evidences.findById(evidenceId)
                .filter(item -> item.getServiceRequest().getId().equals(requestId))
                .orElseThrow(() -> new NotFoundException("Service evidence not found"));
        boolean staff = user.getRole() == Role.ADMIN || user.getRole() == Role.VERIFIER;
        if (!staff && !evidence.getUploadedBy().getId().equals(user.getId()))
            throw new ForbiddenException("Only the uploader can delete this evidence");
        evidences.delete(evidence);
        storage.delete(evidence.getPublicId());
    }
    private ServiceRequest requireRequest(UUID id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Service request not found"));
    }
    private void requireParticipant(ServiceRequest request, User user, boolean allowStaff) {
        boolean participant = request.getClient().getId().equals(user.getId())
                || request.getTechnician() != null && request.getTechnician().getId().equals(user.getId());
        boolean staff = allowStaff && (user.getRole() == Role.ADMIN || user.getRole() == Role.VERIFIER);
        if (!participant && !staff) throw new ForbiddenException("Service evidence is private");
        if (participant && user.getRole() != Role.CLIENT && user.getRole() != Role.TECHNICIAN)
            throw new ForbiddenException("Only clients and technicians can upload evidence");
    }
    private ServiceEvidenceResponse map(ServiceEvidence item) {
        return new ServiceEvidenceResponse(item.getId(), item.getServiceRequest().getId(),
                item.getUploadedBy().getId(), item.getUploadedBy().getFullName(), item.getUploadedByRole(),
                item.getEvidenceType(), item.getFileUrl(), item.getDescription(), item.getCreatedAt());
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
