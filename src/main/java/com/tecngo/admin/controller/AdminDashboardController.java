package com.tecngo.admin.controller;

import com.tecngo.admin.dto.AdminDashboardResponse;
import com.tecngo.payments.repository.PaymentRepository;
import com.tecngo.technicians.entity.TechnicianStatus;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
    private final UserRepository users;
    private final TechnicianProfileRepository technicians;
    private final PaymentRepository payments;

    @GetMapping
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                users.count(),
                technicians.countByStatus(TechnicianStatus.PENDING),
                users.countByVerificationStatus(VerificationStatus.PENDING_VERIFICATION),
                payments.count());
    }
}
