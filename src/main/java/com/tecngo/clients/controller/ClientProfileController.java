package com.tecngo.clients.controller;

import com.tecngo.users.dto.UserProfileRequest;
import com.tecngo.users.dto.UserProfileResponse;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/clients/me/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientProfileController {
    private final UserService service;

    @GetMapping
    public UserProfileResponse profile(@AuthenticationPrincipal User user) {
        return service.profile(user);
    }

    @PutMapping
    public UserProfileResponse update(@Valid @RequestBody UserProfileRequest request,
                                      @AuthenticationPrincipal User user) {
        return service.updateProfile(user, request);
    }
}
