package com.tecngo.compliance.service;

import com.tecngo.compliance.dto.AccessAuditResponse;
import com.tecngo.compliance.entity.*;
import com.tecngo.compliance.repository.ComplianceAccessAuditRepository;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplianceAuditService {
    private final ComplianceAccessAuditRepository audits;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User actor, User subject, String resourceType, String resourceId,
                       String action, AuditOutcome outcome, String correlationId,
                       String ipHash, String details) {
        audits.save(ComplianceAccessAudit.builder()
                .actor(actor).subject(subject).resourceType(resourceType)
                .resourceId(clean(resourceId, 255)).action(clean(action, 120))
                .outcome(outcome).correlationId(clean(correlationId, 100))
                .ipHash(clean(ipHash, 64)).details(clean(details, 1000)).build());
    }

    @Transactional(readOnly = true)
    public List<AccessAuditResponse> recent(int limit) {
        return audits.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 500)))
                .stream().map(this::map).toList();
    }

    private AccessAuditResponse map(ComplianceAccessAudit item) {
        return new AccessAuditResponse(item.getId(), id(item.getActor()), id(item.getSubject()),
                item.getResourceType(), item.getResourceId(), item.getAction(), item.getOutcome(),
                item.getCorrelationId(), item.getDetails(), item.getCreatedAt());
    }

    private java.util.UUID id(User user) {
        return user == null ? null : user.getId();
    }

    private String clean(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
