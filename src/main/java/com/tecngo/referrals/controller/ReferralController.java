package com.tecngo.referrals.controller;
import com.tecngo.referrals.dto.*;
import com.tecngo.referrals.service.ReferralService;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReferralController {
    private final ReferralService service;
    @GetMapping("/v1/referrals/validate/{code}")
    public ReferralValidationResponse validate(@PathVariable String code) { return service.validate(code); }
    @GetMapping("/v1/technicians/me/referral-code")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ReferralCodeResponse mine(@AuthenticationPrincipal User user) { return service.mine(user); }
    @GetMapping("/v1/technicians/me/referrals")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public List<ReferralRegistrationResponse> referrals(@AuthenticationPrincipal User user) { return service.myReferrals(user); }
    @GetMapping("/v1/technicians/me/referral-rewards")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public List<ReferralRewardResponse> rewards(@AuthenticationPrincipal User user) { return service.myRewards(user); }
}
