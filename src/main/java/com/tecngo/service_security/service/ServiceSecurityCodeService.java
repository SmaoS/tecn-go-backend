package com.tecngo.service_security.service;

import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.service_requests.service.ServiceRequestAccessPolicy;
import com.tecngo.service_requests.service.ServiceRequestAssembler;
import com.tecngo.service_requests.dto.ServiceRequestResponse;
import com.tecngo.service_security.dto.SecurityCodeResponse;
import com.tecngo.service_security.dto.VerifyTechnicianResponse;
import com.tecngo.service_security.entity.*;
import com.tecngo.service_security.repository.ServiceSecurityAuditRepository;
import com.tecngo.service_security.repository.ServiceSecurityCodeRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceSecurityCodeService {
    private final ServiceSecurityCodeRepository codes;
    private final ServiceSecurityAuditRepository audits;
    private final ServiceRequestRepository requests;
    private final SystemParameterService parameters;
    private final ServiceRequestAccessPolicy access;
    private final ServiceSecurityNotifier notifier;
    private final ServiceRequestAssembler assembler;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void generateForAssignedRequest(ServiceRequest request) {
        if (!parameters.serviceSecurityCodeEnabled() || request.getTechnician() == null) return;
        createCode(request, request.getTechnician(), ServiceSecurityAuditAction.SECURITY_CODE_GENERATED, 0);
    }

    @Transactional(readOnly = true)
    public SecurityCodeResponse currentCode(UUID requestId, User technician) {
        access.requireRole(technician, Role.TECHNICIAN);
        ServiceRequest request = requests.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        access.requireAssignedTechnician(request, technician);
        ServiceSecurityCode code = codes.findFirstByServiceRequestIdAndStatusOrderByCreatedAtDesc(
                        requestId, ServiceSecurityCodeStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Security code not found"));
        if (code.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("SECURITY_CODE_EXPIRED", "Security code has expired");
        }
        return new SecurityCodeResponse(code.getCodePlain(), code.getExpiresAt(), code.getStatus(),
                code.getVerificationAttempts(), code.getMaxAttempts(), code.getRegeneratedCount(),
                parameters.serviceSecurityMaxRegenerations());
    }

    @Transactional
    public SecurityCodeResponse regenerate(UUID requestId, User technician) {
        access.requireRole(technician, Role.TECHNICIAN);
        access.requireCriticalAccess(technician);
        ServiceRequest request = requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        access.requireAssignedTechnician(request, technician);
        ServiceSecurityCode current = codes.findFirstByServiceRequestIdAndStatus(
                requestId, ServiceSecurityCodeStatus.ACTIVE).orElse(null);
        int regeneratedCount = current == null
                ? codes.findFirstByServiceRequestIdOrderByCreatedAtDesc(requestId)
                        .map(ServiceSecurityCode::getRegeneratedCount).orElse(0)
                : current.getRegeneratedCount() + 1;
        if (regeneratedCount > parameters.serviceSecurityMaxRegenerations()) {
            throw new ConflictException("SECURITY_CODE_TOO_MANY_REGENERATIONS",
                    "Security code regeneration limit reached");
        }
        String plain = createCode(request, technician,
                ServiceSecurityAuditAction.SECURITY_CODE_REGENERATED, regeneratedCount);
        ServiceSecurityCode active = codes.findFirstByServiceRequestIdAndStatusOrderByCreatedAtDesc(
                        requestId, ServiceSecurityCodeStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Security code not found"));
        return new SecurityCodeResponse(plain, active.getExpiresAt(), active.getStatus(),
                active.getVerificationAttempts(), active.getMaxAttempts(), active.getRegeneratedCount(),
                parameters.serviceSecurityMaxRegenerations());
    }

    @Transactional
    public VerifyTechnicianResponse verify(UUID requestId, String rawCode, User client) {
        access.requireRole(client, Role.CLIENT);
        access.requireCriticalAccess(client);
        ServiceRequest request = requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        access.requireClientOwner(request, client);
        ServiceSecurityCode code = codes.findFirstByServiceRequestIdAndStatus(
                        requestId, ServiceSecurityCodeStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Security code not found"));
        if (code.getExpiresAt().isBefore(Instant.now())) {
            code.setStatus(ServiceSecurityCodeStatus.EXPIRED);
            audit(request, client, ServiceSecurityAuditAction.SECURITY_CODE_FAILED, code.getVerificationAttempts());
            throw new ConflictException("SECURITY_CODE_EXPIRED", "Security code has expired");
        }
        if (code.getVerificationAttempts() >= code.getMaxAttempts()) {
            audit(request, client, ServiceSecurityAuditAction.SECURITY_CODE_LOCKED, code.getVerificationAttempts());
            throw new ConflictException("SECURITY_CODE_TOO_MANY_ATTEMPTS",
                    "Security code has too many failed attempts");
        }
        if (!code.getCodeHash().equals(hash(clean(rawCode)))) {
            code.setVerificationAttempts(code.getVerificationAttempts() + 1);
            audit(request, client, ServiceSecurityAuditAction.SECURITY_CODE_FAILED,
                    code.getVerificationAttempts());
            if (code.getVerificationAttempts() >= code.getMaxAttempts()) {
                audit(request, client, ServiceSecurityAuditAction.SECURITY_CODE_LOCKED,
                        code.getVerificationAttempts());
                throw new ConflictException("SECURITY_CODE_TOO_MANY_ATTEMPTS",
                        "Security code has too many failed attempts");
            }
            throw new ConflictException("SECURITY_CODE_INVALID", "Security code is invalid");
        }
        code.setStatus(ServiceSecurityCodeStatus.VERIFIED);
        code.setVerifiedAt(Instant.now());
        request.setStatus(RequestStatus.SECURITY_VERIFIED);
        audit(request, client, ServiceSecurityAuditAction.SECURITY_CODE_VERIFIED,
                code.getVerificationAttempts());
        notifier.securityVerified(request);
        return new VerifyTechnicianResponse(true, "Técnico verificado correctamente.");
    }

    @Transactional
    public void cancelActive(ServiceRequest request, User actor) {
        codes.findFirstByServiceRequestIdAndStatus(request.getId(), ServiceSecurityCodeStatus.ACTIVE)
                .ifPresent(code -> {
                    code.setStatus(ServiceSecurityCodeStatus.CANCELLED);
                    audit(request, actor, ServiceSecurityAuditAction.SECURITY_CODE_CANCELLED,
                            code.getVerificationAttempts());
                });
    }

    @Transactional(readOnly = true)
    public boolean verified(ServiceRequest request) {
        return request.getStatus() == RequestStatus.SECURITY_VERIFIED
                || request.getStatus() == RequestStatus.IN_PROGRESS
                || request.getStatus() == RequestStatus.COMPLETED
                || request.getStatus() == RequestStatus.PAID;
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse detail(UUID requestId, User user) {
        ServiceRequest request = requests.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        access.requireParticipant(request, user);
        return assembler.response(request);
    }

    private String createCode(ServiceRequest request, User technician,
                              ServiceSecurityAuditAction action, int regeneratedCount) {
        codes.findFirstByServiceRequestIdAndStatus(request.getId(), ServiceSecurityCodeStatus.ACTIVE)
                .ifPresent(current -> current.setStatus(ServiceSecurityCodeStatus.CANCELLED));
        String plain = generatePlainCode();
        codes.save(ServiceSecurityCode.builder()
                .serviceRequest(request)
                .technician(technician)
                .client(request.getClient())
                .codePlain(plain)
                .codeHash(hash(plain))
                .status(ServiceSecurityCodeStatus.ACTIVE)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(parameters.serviceSecurityCodeExpirationMinutes(), ChronoUnit.MINUTES))
                .maxAttempts(parameters.serviceSecurityMaxAttempts())
                .regeneratedCount(regeneratedCount)
                .build());
        audit(request, technician, action, 0);
        return plain;
    }

    private String generatePlainCode() {
        int length = parameters.serviceSecurityCodeLength();
        int bound = (int) Math.pow(10, length);
        int minimum = (int) Math.pow(10, length - 1);
        return String.valueOf(minimum + random.nextInt(bound - minimum));
    }

    private void audit(ServiceRequest request, User actor,
                       ServiceSecurityAuditAction action, int attempts) {
        audits.save(ServiceSecurityAudit.builder()
                .serviceRequest(request)
                .technician(request.getTechnician())
                .client(request.getClient())
                .actor(actor)
                .action(action)
                .attempts(attempts)
                .build());
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

}
