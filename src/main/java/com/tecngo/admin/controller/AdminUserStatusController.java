package com.tecngo.admin.controller;

import com.tecngo.users.dto.InactivateUserRequest;
import com.tecngo.users.dto.InactiveUserResponse;
import com.tecngo.admin.dto.AdminUserSearchResponse;
import com.tecngo.admin.service.AdminUserSearchService;
import com.tecngo.users.entity.AccountStatus;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.UserAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserStatusController {
    private final UserAccessService service;
    private final AdminUserSearchService searchService;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','VERIFIER')")
    public Page<AdminUserSearchResponse> search(@RequestParam(required = false) Role role,
                                                @RequestParam(required = false) AccountStatus status,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                Instant createdFrom,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                Instant createdTo,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "25") int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        return searchService.search(role, status, createdFrom, createdTo, name,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InactiveUserResponse> inactive(@AuthenticationPrincipal User admin) {
        return service.inactive(admin);
    }

    @PutMapping("/{id}/inactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public InactiveUserResponse inactivate(@PathVariable UUID id,
                                           @Valid @RequestBody InactivateUserRequest request,
                                           @AuthenticationPrincipal User admin) {
        return service.inactivate(id, request, admin);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public InactiveUserResponse activate(@PathVariable UUID id, @AuthenticationPrincipal User admin) {
        return service.activate(id, admin);
    }
}
