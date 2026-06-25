package com.tecngo.compliance.controller;

import com.tecngo.compliance.dto.*;
import com.tecngo.compliance.service.ComplianceDataService;
import com.tecngo.observability.CorrelationIdFilter;
import com.tecngo.users.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/users/me")
@RequiredArgsConstructor
public class ComplianceUserController {
    private final ComplianceDataService service;

    @PostMapping("/data-export")
    public DataRequestResponse export(@AuthenticationPrincipal User user, HttpServletRequest request) {
        return service.requestExport(user, correlation(request));
    }

    @PostMapping("/data-export-request")
    public DataRequestResponse exportRequest(@AuthenticationPrincipal User user, HttpServletRequest request) {
        return service.requestExport(user, correlation(request));
    }

    @GetMapping("/data-export-requests")
    public List<DataRequestResponse> exportRequests(@AuthenticationPrincipal User user) {
        return service.exportRequests(user);
    }

    @PostMapping("/data-anonymization")
    public DataRequestResponse anonymization(@Valid @RequestBody AnonymizationRequest request,
                                             @AuthenticationPrincipal User user) {
        return service.requestAnonymization(user, request.reason());
    }

    private String correlation(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.MDC_KEY);
        return value == null ? null : value.toString();
    }
}
