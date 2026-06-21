package com.tecngo.compliance.service;

import com.tecngo.compliance.dto.*;
import com.tecngo.compliance.entity.*;
import com.tecngo.compliance.repository.ComplianceIncidentRepository;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IncidentService {
    private final ComplianceIncidentRepository incidents;
    private final UserRepository users;

    @Transactional
    public IncidentResponse create(IncidentRequest request, User reporter) {
        return map(incidents.save(ComplianceIncident.builder()
                .title(request.title().trim()).description(request.description().trim())
                .severity(request.severity()).status(IncidentStatus.OPEN)
                .detectedAt(request.detectedAt() == null ? Instant.now() : request.detectedAt())
                .reportedBy(reporter).assignedTo(user(request.assignedToUserId())).build()));
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> list(IncidentStatus status) {
        var items = status == null
                ? incidents.findAllByOrderByDetectedAtDesc()
                : incidents.findByStatusOrderByDetectedAtDesc(status);
        return items.stream().map(this::map).toList();
    }

    @Transactional
    public IncidentResponse update(UUID id, IncidentUpdateRequest request) {
        ComplianceIncident incident = incidents.findById(id)
                .orElseThrow(() -> new NotFoundException("Compliance incident not found"));
        incident.setStatus(request.status());
        if (request.severity() != null) incident.setSeverity(request.severity());
        incident.setAssignedTo(user(request.assignedToUserId()));
        incident.setResolutionSummary(clean(request.resolutionSummary()));
        Instant now = Instant.now();
        if (request.status() == IncidentStatus.CONTAINED && incident.getContainedAt() == null) {
            incident.setContainedAt(now);
        }
        if (request.status() == IncidentStatus.RESOLVED) {
            if (incident.getContainedAt() == null) incident.setContainedAt(now);
            incident.setResolvedAt(now);
        }
        return map(incidents.save(incident));
    }

    private User user(UUID id) {
        if (id == null) return null;
        return users.findById(id).orElseThrow(() -> new NotFoundException("Assigned user not found"));
    }

    private IncidentResponse map(ComplianceIncident item) {
        return new IncidentResponse(item.getId(), item.getTitle(), item.getDescription(),
                item.getSeverity(), item.getStatus(), item.getDetectedAt(), item.getContainedAt(),
                item.getResolvedAt(), item.getCreatedAt(), item.getReportedBy().getId(),
                item.getAssignedTo() == null ? null : item.getAssignedTo().getId(),
                item.getResolutionSummary());
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= 4000 ? trimmed : trimmed.substring(0, 4000);
    }
}
