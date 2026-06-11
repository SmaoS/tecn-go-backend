package com.tecngo.admin.controller;

import com.tecngo.users.dto.InactivateUserRequest;
import com.tecngo.users.dto.InactiveUserResponse;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.UserAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserStatusController {
    private final UserAccessService service;

    @GetMapping("/inactive")
    public List<InactiveUserResponse> inactive(@AuthenticationPrincipal User admin) {
        return service.inactive(admin);
    }

    @PutMapping("/{id}/inactivate")
    public InactiveUserResponse inactivate(@PathVariable UUID id,
                                           @Valid @RequestBody InactivateUserRequest request,
                                           @AuthenticationPrincipal User admin) {
        return service.inactivate(id, request, admin);
    }

    @PutMapping("/{id}/activate")
    public InactiveUserResponse activate(@PathVariable UUID id, @AuthenticationPrincipal User admin) {
        return service.activate(id, admin);
    }
}
