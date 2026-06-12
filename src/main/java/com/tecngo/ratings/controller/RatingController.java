package com.tecngo.ratings.controller;

import com.tecngo.ratings.dto.CreateRatingRequest;
import com.tecngo.ratings.dto.RatingResponse;
import com.tecngo.ratings.dto.TechnicianRatingSummary;
import com.tecngo.ratings.dto.RatingStatusResponse;
import com.tecngo.ratings.service.RatingService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RatingController {
    private final RatingService service;

    @PostMapping("/v1/service-requests/{id}/ratings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENT','TECHNICIAN')")
    public RatingResponse create(@PathVariable UUID id,
                                 @Valid @RequestBody CreateRatingRequest request,
                                 @AuthenticationPrincipal User user) {
        return service.create(id, request, user);
    }

    @GetMapping("/v1/technicians/{id}/ratings")
    public List<RatingResponse> ratings(@PathVariable UUID id) {
        return service.technicianRatings(id);
    }

    @GetMapping("/v1/technicians/{id}/summary")
    public TechnicianRatingSummary summary(@PathVariable UUID id) {
        return service.technicianSummary(id);
    }

    @GetMapping("/v1/service-requests/{id}/ratings/me")
    @PreAuthorize("hasAnyRole('CLIENT','TECHNICIAN')")
    public RatingStatusResponse status(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return service.status(id, user);
    }
}
