package com.tecngo.admin.controller;

import com.tecngo.users.dto.UserVerificationResponse;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'VERIFIER')")
public class AdminUserVerificationController {
    private final VerificationService service;

    @GetMapping("/pending-documents")
    public List<UserVerificationResponse> pending() {
        return service.pending();
    }

    @PutMapping("/{id}/verify-documents")
    public UserVerificationResponse verify(@PathVariable UUID id,
                                           @AuthenticationPrincipal User reviewer) {
        return service.verify(id, reviewer);
    }

    @PutMapping("/{id}/reject-documents")
    public UserVerificationResponse reject(@PathVariable UUID id,
                                           @AuthenticationPrincipal User reviewer) {
        return service.reject(id, reviewer);
    }
}
