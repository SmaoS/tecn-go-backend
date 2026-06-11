package com.tecngo.reports.controller;
import com.tecngo.reports.dto.*;
import com.tecngo.reports.service.UserReportService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/v1/service-requests/{requestId}/reports") @RequiredArgsConstructor
public class UserReportController {
    private final UserReportService service;
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public UserReportResponse create(@PathVariable UUID requestId, @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal User user) { return service.create(requestId, request, user); }
}
