package com.tecngo.service_evidence.controller;

import com.tecngo.service_evidence.dto.ServiceEvidenceResponse;
import com.tecngo.service_evidence.service.ServiceEvidenceService;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/evidences")
@RequiredArgsConstructor
public class AdminServiceEvidenceController {
    private final ServiceEvidenceService service;

    @GetMapping
    public List<ServiceEvidenceResponse> list(@AuthenticationPrincipal User user) {
        return service.listAll(user);
    }
}
