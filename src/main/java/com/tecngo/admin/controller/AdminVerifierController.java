package com.tecngo.admin.controller;

import com.tecngo.admin.dto.CreateVerifierRequest;
import com.tecngo.admin.dto.VerifierResponse;
import com.tecngo.admin.service.VerifierService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/verifiers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVerifierController {
    private final VerifierService service;

    @GetMapping
    public List<VerifierResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VerifierResponse create(@Valid @RequestBody CreateVerifierRequest request,
                                   @AuthenticationPrincipal User admin) {
        return service.create(request, admin);
    }
}
