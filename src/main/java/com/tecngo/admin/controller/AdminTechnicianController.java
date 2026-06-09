package com.tecngo.admin.controller;

import com.tecngo.technicians.dto.TechnicianProfileResponse;
import com.tecngo.technicians.entity.TechnicianStatus;
import com.tecngo.technicians.service.TechnicianProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/technicians")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTechnicianController {
    private final TechnicianProfileService service;

    @GetMapping("/pending")
    public List<TechnicianProfileResponse> pending() {
        return service.pending();
    }

    @PutMapping("/{id}/approve")
    public TechnicianProfileResponse approve(@PathVariable UUID id) {
        return service.review(id, TechnicianStatus.APPROVED);
    }

    @PutMapping("/{id}/reject")
    public TechnicianProfileResponse reject(@PathVariable UUID id) {
        return service.review(id, TechnicianStatus.REJECTED);
    }
}
