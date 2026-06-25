package com.tecngo.compliance.controller;

import com.tecngo.compliance.dto.*;
import com.tecngo.compliance.entity.*;
import com.tecngo.compliance.service.*;
import com.tecngo.observability.CorrelationIdFilter;
import com.tecngo.users.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/admin/compliance")
@RequiredArgsConstructor
public class ComplianceAdminController {
    private final ComplianceDataService data;
    private final RetentionService retention;
    private final IncidentService incidents;
    private final ComplianceAuditService audits;

    @GetMapping("/data-requests")
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public List<DataRequestResponse> dataRequests(@RequestParam(required = false) DataRequestStatus status) {
        return data.requests(status);
    }

    @GetMapping("/data-export-requests")
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public List<DataRequestResponse> dataExportRequests(@RequestParam(required = false) DataRequestStatus status) {
        return data.exportRequests(status);
    }

    @PutMapping("/data-requests/{id}/approve-anonymization")
    @PreAuthorize("hasRole('ADMIN')")
    public DataRequestResponse approve(@PathVariable UUID id, @AuthenticationPrincipal User admin,
                                       HttpServletRequest request) {
        return data.approveAnonymization(id, admin, correlation(request));
    }

    @PutMapping("/data-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public DataRequestResponse reject(@PathVariable UUID id,
                                      @Valid @RequestBody AnonymizationRequest request,
                                      @AuthenticationPrincipal User admin) {
        return data.reject(id, request.reason(), admin);
    }

    @PutMapping("/data-export-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public DataRequestResponse approveExport(@PathVariable UUID id,
                                             @AuthenticationPrincipal User reviewer,
                                             HttpServletRequest request) {
        return data.approveExport(id, reviewer, correlation(request));
    }

    @PutMapping("/data-export-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public DataRequestResponse rejectExport(@PathVariable UUID id,
                                            @Valid @RequestBody AnonymizationRequest request,
                                            @AuthenticationPrincipal User reviewer) {
        return data.reject(id, request.reason(), reviewer);
    }

    @GetMapping("/retention-policies")
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public List<RetentionPolicyResponse> policies() {
        return retention.policies();
    }

    @PutMapping("/retention-policies/{category}")
    @PreAuthorize("hasRole('ADMIN')")
    public RetentionPolicyResponse updatePolicy(@PathVariable String category,
                                                @Valid @RequestBody RetentionPolicyRequest request,
                                                @AuthenticationPrincipal User admin) {
        return retention.update(category, request, admin);
    }

    @PostMapping("/retention/run")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> runRetention() {
        return retention.execute();
    }

    @PostMapping("/incidents")
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public IncidentResponse createIncident(@Valid @RequestBody IncidentRequest request,
                                           @AuthenticationPrincipal User reporter) {
        return incidents.create(request, reporter);
    }

    @GetMapping("/incidents")
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public List<IncidentResponse> incidents(@RequestParam(required = false) IncidentStatus status) {
        return incidents.list(status);
    }

    @PutMapping("/incidents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public IncidentResponse updateIncident(@PathVariable UUID id,
                                           @Valid @RequestBody IncidentUpdateRequest request) {
        return incidents.update(id, request);
    }

    @GetMapping("/access-audits")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccessAuditResponse> accessAudits(@RequestParam(defaultValue = "100") int limit) {
        return audits.recent(limit);
    }

    private String correlation(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.MDC_KEY);
        return value == null ? null : value.toString();
    }
}
