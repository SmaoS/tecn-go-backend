package com.tecngo.service_requests.service;

import com.tecngo.payments.entity.Payment;
import com.tecngo.payments.entity.PaymentMethod;
import com.tecngo.payments.entity.PaymentStatus;
import com.tecngo.payments.repository.PaymentRepository;
import com.tecngo.payments.service.PlatformFeeCalculator;
import com.tecngo.referrals.entity.ReferralReward;
import com.tecngo.referrals.service.ReferralService;
import com.tecngo.reports.entity.ReportReason;
import com.tecngo.reports.entity.ReportSeverity;
import com.tecngo.reports.entity.UserReport;
import com.tecngo.reports.repository.UserReportRepository;
import com.tecngo.service_requests.dto.ServiceRequestResponse;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.technician_wallet.service.TechnicianWalletService;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceLifecycleService {
    private final ServiceRequestRepository requests;
    private final PaymentRepository payments;
    private final PlatformFeeCalculator feeCalculator;
    private final ReferralService referrals;
    private final UserReportRepository reports;
    private final TechnicianWalletService wallets;
    private final SystemParameterService parameters;
    private final ServiceRequestAccessPolicy access;
    private final ServiceRequestAssembler assembler;
    private final ServiceRequestNotifier notifier;

    @Transactional
    public ServiceRequestResponse updateStatus(UUID id, RequestStatus nextStatus, User user) {
        access.requireCriticalAccess(user);
        ServiceRequest request = find(id);
        if (nextStatus == RequestStatus.CANCELLED && user.isActiveAs(Role.CLIENT)) {
            access.requireClientOwner(request, user);
            if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.PAID
                    || request.getStatus() == RequestStatus.CANCELLED) {
                throw new ConflictException("Completed, paid or cancelled requests cannot be cancelled");
            }
            request.setStatus(RequestStatus.CANCELLED);
            notifier.cancelled(request, user);
            return assembler.response(request);
        }
        access.requireRole(user, Role.TECHNICIAN);
        access.requireAssignedTechnician(request, user);
        if (!validTransition(request.getStatus(), nextStatus)) {
            throw new ConflictException("Invalid service request status transition");
        }
        request.setStatus(nextStatus);
        if (nextStatus == RequestStatus.COMPLETED) incrementCompleted(request);
        notifier.statusChanged(request, nextStatus);
        return assembler.response(request);
    }

    @Transactional
    public ServiceRequestResponse technicianComplete(UUID id, boolean paymentReceived,
                                                     PaymentMethod paymentMethod, String comment,
                                                     User technician) {
        access.requireRole(technician, Role.TECHNICIAN);
        access.requireCriticalAccess(technician);
        ServiceRequest request = requests.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        access.requireAssignedTechnician(request, technician);
        validateClosable(request);
        if (paymentReceived) {
            createPaidPayment(request,
                    paymentMethod == null ? request.getRequestedPaymentMethod() : paymentMethod);
            request.setStatus(RequestStatus.PAID);
            incrementCompleted(request);
            request.getClient().setPaidServicesCount(request.getClient().getPaidServicesCount() + 1);
            request.getTechnician().setPaidServicesCount(request.getTechnician().getPaidServicesCount() + 1);
            notifier.paymentConfirmed(request);
        } else {
            request.setStatus(RequestStatus.PAYMENT_DISPUTE);
            reports.save(UserReport.builder()
                    .serviceRequest(request)
                    .reporter(technician)
                    .reported(request.getClient())
                    .reporterRole(technician.getRole())
                    .reportedRole(request.getClient().getRole())
                    .reason(ReportReason.PAYMENT_ISSUE)
                    .description(clean(comment) == null
                            ? "El técnico reportó que el cliente no pagó el valor acordado."
                            : clean(comment))
                    .severity(ReportSeverity.HIGH)
                    .build());
            notifier.paymentDisputed(request);
        }
        return assembler.response(request);
    }

    private void createPaidPayment(ServiceRequest request, PaymentMethod method) {
        if (payments.existsByServiceRequestId(request.getId())) {
            throw new ConflictException("Service request is already paid");
        }
        BigDecimal amount = request.getFinalPrice();
        BigDecimal percentage = parameters.platformCommissionPercentage();
        ReferralReward reward = referrals.useAvailableReward(request.getTechnician(), request, percentage);
        boolean commissionWaived = reward != null;
        BigDecimal effectivePercentage = commissionWaived ? BigDecimal.ZERO : percentage;
        Payment payment = payments.save(Payment.builder()
                .serviceRequest(request)
                .client(request.getClient())
                .technician(request.getTechnician())
                .amount(amount)
                .platformFee(feeCalculator.fee(amount, effectivePercentage))
                .platformCommissionPercentage(effectivePercentage)
                .technicianAmount(feeCalculator.technicianAmount(amount, effectivePercentage))
                .commissionWaived(commissionWaived)
                .commissionWaivedReason(commissionWaived ? "REFERRAL_REWARD" : null)
                .referralReward(reward)
                .status(PaymentStatus.PAID)
                .method(method == null ? PaymentMethod.CASH : method)
                .build());
        payment.setTechnicianWalletTransactionId(wallets.debitCommissionIfEnabled(payment));
    }

    private void validateClosable(ServiceRequest request) {
        if (request.getStatus() == RequestStatus.PAID
                || request.getStatus() == RequestStatus.PAYMENT_DISPUTE
                || request.getStatus() == RequestStatus.CANCELLED) {
            throw new ConflictException("Service request is already closed");
        }
        if (!Set.of(RequestStatus.QUOTE_ACCEPTED, RequestStatus.ON_THE_WAY, RequestStatus.ARRIVED,
                RequestStatus.IN_PROGRESS, RequestStatus.COMPLETED).contains(request.getStatus())) {
            throw new ConflictException("Service request cannot be completed from current status");
        }
        if (request.getFinalPrice() == null || request.getFinalPrice().signum() <= 0) {
            throw new ConflictException("Service request does not have a final price");
        }
    }

    private boolean validTransition(RequestStatus current, RequestStatus next) {
        return switch (current) {
            case QUOTE_ACCEPTED -> next == RequestStatus.ON_THE_WAY;
            case ON_THE_WAY -> next == RequestStatus.ARRIVED;
            case ARRIVED -> next == RequestStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == RequestStatus.COMPLETED;
            default -> false;
        };
    }

    private void incrementCompleted(ServiceRequest request) {
        request.getClient().setCompletedServicesCount(request.getClient().getCompletedServicesCount() + 1);
        request.getTechnician().setCompletedServicesCount(request.getTechnician().getCompletedServicesCount() + 1);
    }

    private ServiceRequest find(UUID id) {
        return requests.findById(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
