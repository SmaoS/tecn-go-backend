package com.tecngo.payments.service;

import com.tecngo.payments.dto.FinancialSummaryResponse;
import com.tecngo.payments.dto.PaymentResponse;
import com.tecngo.payments.entity.Payment;
import com.tecngo.payments.entity.PaymentMethod;
import com.tecngo.payments.entity.PaymentStatus;
import com.tecngo.payments.repository.PaymentRepository;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.service.UserAccessService;
import com.tecngo.legal.service.LegalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.tecngo.referrals.entity.ReferralReward;
import com.tecngo.referrals.service.ReferralService;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository payments;
    private final ServiceRequestRepository requests;
    private final PlatformFeeCalculator feeCalculator;
    private final SystemParameterService parameters;
    private final UserRepository users;
    private final UserAccessService userAccess;
    private final LegalService legal;
    private final ReferralService referrals;

    @Transactional
    public PaymentResponse payCash(UUID requestId, User client) {
        requireRole(client, Role.CLIENT);
        userAccess.requireActive(client);
        legal.requireAccepted(client);
        ServiceRequest request = requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        if (!request.getClient().getId().equals(client.getId())) {
            throw new ForbiddenException("Only the client owner can pay this service");
        }
        if (request.getStatus() != RequestStatus.COMPLETED) {
            throw new ConflictException("Only completed services can be paid");
        }
        if (payments.existsByServiceRequestId(requestId)) {
            throw new ConflictException("Service request is already paid");
        }
        if (request.getTechnician() == null || request.getFinalPrice() == null) {
            throw new ConflictException("Service request does not have a final price or technician");
        }

        BigDecimal amount = request.getFinalPrice();
        BigDecimal percentage = parameters.platformCommissionPercentage();
        ReferralReward reward = referrals.useAvailableReward(request.getTechnician(), request, percentage);
        boolean commissionWaived = reward != null;
        BigDecimal effectivePercentage = commissionWaived ? BigDecimal.ZERO : percentage;
        BigDecimal platformFee = feeCalculator.fee(amount, effectivePercentage);
        Payment payment = payments.save(Payment.builder()
                .serviceRequest(request)
                .client(client)
                .technician(request.getTechnician())
                .amount(amount)
                .platformFee(platformFee)
                .platformCommissionPercentage(effectivePercentage)
                .technicianAmount(feeCalculator.technicianAmount(amount, effectivePercentage))
                .commissionWaived(commissionWaived)
                .commissionWaivedReason(commissionWaived ? "REFERRAL_REWARD" : null)
                .referralReward(reward)
                .status(PaymentStatus.PAID)
                .method(PaymentMethod.CASH)
                .build());
        request.setStatus(RequestStatus.PAID);
        client.setPaidServicesCount(client.getPaidServicesCount() + 1);
        request.getTechnician().setPaidServicesCount(request.getTechnician().getPaidServicesCount() + 1);
        users.save(client);
        users.save(request.getTechnician());
        return map(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> clientHistory(User client) {
        requireRole(client, Role.CLIENT);
        return payments.findByClientIdOrderByCreatedAtDesc(client.getId()).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse technicianEarnings(User technician) {
        requireRole(technician, Role.TECHNICIAN);
        return summary(payments.findByTechnicianIdOrderByCreatedAtDesc(technician.getId()));
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse adminSummary(User admin) {
        requireRole(admin, Role.ADMIN);
        return summary(payments.findAllByOrderByCreatedAtDesc());
    }

    private FinancialSummaryResponse summary(List<Payment> items) {
        List<PaymentResponse> responses = items.stream().map(this::map).toList();
        BigDecimal total = items.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fees = items.stream().map(Payment::getPlatformFee).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal technician = items.stream().map(Payment::getTechnicianAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FinancialSummaryResponse(total, fees, technician, items.size(), responses);
    }

    private PaymentResponse map(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getServiceRequest().getId(),
                payment.getClient().getId(), payment.getClient().getFullName(),
                payment.getTechnician().getId(), payment.getTechnician().getFullName(),
                payment.getAmount(), payment.getPlatformFee(), payment.getTechnicianAmount(),
                payment.getPlatformCommissionPercentage(),
                payment.getStatus(), payment.getMethod(), payment.isCommissionWaived(),
                payment.getCommissionWaivedReason(),
                payment.getReferralReward() == null ? null : payment.getReferralReward().getId(),
                payment.getCreatedAt());
    }

    private void requireRole(User user, Role role) {
        if (user.getRole() != role) throw new ForbiddenException("Role " + role + " is required");
    }
}
