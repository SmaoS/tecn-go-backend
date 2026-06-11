package com.tecngo.reports.controller;
import com.tecngo.reports.dto.*;
import com.tecngo.reports.service.UserReportService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/v1/admin/reports") @RequiredArgsConstructor
public class AdminReportController {
    private final UserReportService service;
    @GetMapping public List<UserReportResponse> list(@AuthenticationPrincipal User user) { return service.list(user); }
    @GetMapping("/{id}") public UserReportResponse find(@PathVariable UUID id, @AuthenticationPrincipal User user) { return service.find(id, user); }
    @PutMapping("/{id}/status") public UserReportResponse update(@PathVariable UUID id,
            @Valid @RequestBody UpdateReportRequest request, @AuthenticationPrincipal User user) {
        return service.update(id, request, user);
    }
    @PutMapping("/{id}/resolve") public UserReportResponse resolve(@PathVariable UUID id,
            @Valid @RequestBody UpdateReportRequest request, @AuthenticationPrincipal User user) {
        return service.resolve(id, request, user);
    }
}
