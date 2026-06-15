package com.tecngo.technician_location.controller;

import com.tecngo.technician_location.dto.TechnicianLocationRequest;
import com.tecngo.technician_location.dto.TechnicianLocationResponse;
import com.tecngo.technician_location.dto.NearbyTechnicianResponse;
import com.tecngo.technician_location.service.TechnicianLocationService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class TechnicianLocationController {
    private final TechnicianLocationService service;

    @PutMapping("/technicians/me/location")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public TechnicianLocationResponse update(@Valid @RequestBody TechnicianLocationRequest request,
                                             @AuthenticationPrincipal User user) {
        return service.update(user, request);
    }

    @GetMapping("/technicians/me/location")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public TechnicianLocationResponse mine(@AuthenticationPrincipal User user) {
        return service.mine(user);
    }

    @GetMapping("/admin/technicians/locations")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TechnicianLocationResponse> all() {
        return service.all();
    }

    @GetMapping("/service-requests/{id}/technician-location")
    @PreAuthorize("hasRole('CLIENT')")
    public TechnicianLocationResponse forRequest(@PathVariable UUID id,
                                                 @AuthenticationPrincipal User user) {
        return service.forRequest(id, user);
    }

    @GetMapping("/technicians/nearby")
    @PreAuthorize("hasRole('CLIENT')")
    public List<NearbyTechnicianResponse> nearby(@RequestParam double latitude,
                                                @RequestParam double longitude,
                                                @RequestParam(defaultValue = "25") double radiusKm,
                                                @RequestParam(required = false) UUID cityId) {
        return service.nearby(latitude, longitude, radiusKm, cityId);
    }
}
