package com.tecngo.referrals.controller;
import com.tecngo.referrals.dto.*;
import com.tecngo.referrals.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/v1/admin/referrals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReferralController {
    private final ReferralService service;
    @GetMapping public List<ReferralCodeResponse> codes() { return service.adminCodes(); }
    @PutMapping("/{id}/active") public ReferralCodeResponse active(@PathVariable UUID id, @RequestParam boolean value) { return service.toggle(id, value); }
    @PutMapping("/{id}/regenerate") public ReferralCodeResponse regenerate(@PathVariable UUID id) { return service.regenerate(id); }
    @GetMapping("/technicians/{technicianId}/registrations")
    public List<ReferralRegistrationResponse> registrations(@PathVariable UUID technicianId) { return service.adminReferrals(technicianId); }
    @GetMapping("/technicians/{technicianId}/rewards")
    public List<ReferralRewardResponse> rewards(@PathVariable UUID technicianId) { return service.adminRewards(technicianId); }
}
