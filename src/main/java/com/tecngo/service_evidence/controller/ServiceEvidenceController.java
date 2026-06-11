package com.tecngo.service_evidence.controller;

import com.tecngo.service_evidence.dto.ServiceEvidenceResponse;
import com.tecngo.service_evidence.entity.EvidenceType;
import com.tecngo.service_evidence.service.ServiceEvidenceService;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController @RequestMapping("/v1/service-requests/{requestId}/evidences") @RequiredArgsConstructor
public class ServiceEvidenceController {
    private final ServiceEvidenceService service;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public ServiceEvidenceResponse upload(@PathVariable UUID requestId,
            @RequestPart("file") MultipartFile file, @RequestParam EvidenceType evidenceType,
            @RequestParam(required = false) String description, @AuthenticationPrincipal User user) {
        return service.upload(requestId, evidenceType, description, file, user);
    }
    @GetMapping public List<ServiceEvidenceResponse> list(@PathVariable UUID requestId,
            @AuthenticationPrincipal User user) { return service.list(requestId, user); }
    @DeleteMapping("/{evidenceId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID requestId, @PathVariable UUID evidenceId,
            @AuthenticationPrincipal User user) { service.delete(requestId, evidenceId, user); }
}
