package com.tecngo.compliance.controller;

import com.tecngo.compliance.dto.AnonymizationRequest;
import com.tecngo.compliance.dto.DataRequestResponse;
import com.tecngo.compliance.entity.DataRequestStatus;
import com.tecngo.compliance.service.ComplianceDataService;
import com.tecngo.observability.CorrelationIdFilter;
import com.tecngo.users.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/data-export-requests")
@RequiredArgsConstructor
public class DataExportAdminController {
    private final ComplianceDataService data;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public List<DataRequestResponse> requests(@RequestParam(required = false) DataRequestStatus status) {
        return data.exportRequests(status);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public DataRequestResponse approve(@PathVariable UUID id,
                                       @AuthenticationPrincipal User reviewer,
                                       HttpServletRequest request) {
        return data.approveExport(id, reviewer, correlation(request));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public DataRequestResponse reject(@PathVariable UUID id,
                                      @Valid @RequestBody AnonymizationRequest request,
                                      @AuthenticationPrincipal User reviewer) {
        return data.reject(id, request.reason(), reviewer);
    }

    private String correlation(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.MDC_KEY);
        return value == null ? null : value.toString();
    }
}
