package com.tecngo.technicians.controller;

import com.tecngo.technicians.dto.TechnicianProfileRequest;
import com.tecngo.technicians.dto.TechnicianProfileResponse;
import com.tecngo.technicians.service.TechnicianProfileService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/technicians")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TECHNICIAN')")
public class TechnicianProfileController {
    private final TechnicianProfileService service;

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    public TechnicianProfileResponse create(@Valid @RequestBody TechnicianProfileRequest request,
                                            @AuthenticationPrincipal User user) {
        return service.create(request, user);
    }

    @GetMapping("/me")
    public TechnicianProfileResponse mine(@AuthenticationPrincipal User user) {
        return service.mine(user);
    }

    @PutMapping("/me")
    public TechnicianProfileResponse update(@Valid @RequestBody TechnicianProfileRequest request,
                                            @AuthenticationPrincipal User user) {
        return service.update(request, user);
    }
}
