package com.tecngo.referrals.service;

import com.tecngo.ratings.entity.Rating;
import com.tecngo.referrals.dto.*;
import com.tecngo.referrals.entity.*;
import com.tecngo.referrals.repository.*;
import com.tecngo.service_requests.entity.*;
import com.tecngo.shared.exception.*;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReferralService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final ReferralCodeRepository codes;
    private final ReferralRegistrationRepository registrations;
    private final ReferralRewardRepository rewards;
    private final SystemParameterService parameters;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public ReferralCode ensureCode(User technician) {
        if (technician.getRole() != Role.TECHNICIAN) throw new IllegalArgumentException("Referral codes are only for technicians");
        return codes.findByTechnicianId(technician.getId()).orElseGet(() -> codes.save(ReferralCode.builder()
                .technician(technician).code(nextCode()).active(true).build()));
    }

    @Transactional
    public void register(User referred, String rawCode) {
        if (rawCode == null || rawCode.isBlank()) return;
        if (!parameters.referralProgramEnabled()) throw new ConflictException("El programa de referidos no está disponible");
        ReferralCode code = codes.findByCodeIgnoreCase(rawCode.trim())
                .filter(ReferralCode::isActive)
                .orElseThrow(() -> new IllegalArgumentException("El código de referido no es válido o está inactivo"));
        if (code.getTechnician().getId().equals(referred.getId())) {
            throw new ConflictException("No puedes usar tu propio código de referido");
        }
        if (registrations.findByReferredUserId(referred.getId()).isPresent()) {
            throw new ConflictException("El usuario ya tiene un técnico referidor");
        }
        registrations.save(ReferralRegistration.builder().referralCode(code)
                .referrerTechnician(code.getTechnician()).referredUser(referred)
                .referredUserRole(referred.getRole()).status(ReferralRegistrationStatus.REGISTERED).build());
    }

    @Transactional(readOnly = true)
    public ReferralValidationResponse validate(String rawCode) {
        if (!parameters.referralProgramEnabled()) return new ReferralValidationResponse(false, null, "El programa de referidos está inactivo.");
        return codes.findByCodeIgnoreCase(rawCode.trim()).filter(ReferralCode::isActive)
                .map(code -> new ReferralValidationResponse(true, code.getTechnician().getFullName(),
                        "Código válido. Te invitó: " + code.getTechnician().getFullName() + "."))
                .orElseGet(() -> new ReferralValidationResponse(false, null, "Código de referido inválido o inactivo."));
    }

    @Transactional
    public void qualifyFromRating(ServiceRequest request, Rating rating) {
        if (!parameters.referralProgramEnabled() || request.getStatus() != RequestStatus.PAID
                || rating.getScore() < parameters.referralRequiredRating()) return;
        qualify(request.getClient(), request);
        if (request.getTechnician() != null) qualify(request.getTechnician(), request);
    }

    private void qualify(User referred, ServiceRequest source) {
        registrations.findByReferredUserId(referred.getId()).ifPresent(registration -> {
            if (registration.getStatus() != ReferralRegistrationStatus.REGISTERED) return;
            Instant now = Instant.now();
            registration.setStatus(ReferralRegistrationStatus.QUALIFIED);
            registration.setQualifiedAt(now);
            int days = parameters.referralRewardExpirationDays();
            rewards.save(ReferralReward.builder().technician(registration.getReferrerTechnician())
                    .referralRegistration(registration).sourceServiceRequest(source)
                    .rewardType(ReferralRewardType.FREE_COMMISSION_SERVICE)
                    .status(ReferralRewardStatus.AVAILABLE)
                    .expiresAt(days > 0 ? now.plus(days, ChronoUnit.DAYS) : null).build());
            registration.setStatus(ReferralRegistrationStatus.REWARD_GRANTED);
            registration.setRewardGrantedAt(now);
            registrations.save(registration);
        });
    }

    @Transactional
    public ReferralReward useAvailableReward(User technician, ServiceRequest request, BigDecimal commissionPercentage) {
        if (!parameters.referralProgramEnabled()) return null;
        if (commissionPercentage.signum() <= 0 && parameters.referralRewardOnlyIfCommissionGtZero()) return null;
        List<ReferralReward> available = rewards.findAvailableForUpdate(technician.getId());
        if (available.isEmpty()) return null;
        ReferralReward reward = available.getFirst();
        reward.setStatus(ReferralRewardStatus.USED);
        reward.setUsedServiceRequest(request);
        reward.setUsedAt(Instant.now());
        return rewards.save(reward);
    }

    @Transactional
    public ReferralCodeResponse mine(User technician) { return codeResponse(ensureCode(technician)); }
    @Transactional(readOnly = true)
    public List<ReferralRegistrationResponse> myReferrals(User technician) {
        return registrations.findByReferrerTechnicianIdOrderByCreatedAtDesc(technician.getId()).stream().map(this::registrationResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<ReferralRewardResponse> myRewards(User technician) {
        return rewards.findByTechnicianIdOrderByCreatedAtDesc(technician.getId()).stream().map(this::rewardResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<ReferralCodeResponse> adminCodes() { return codes.findAll().stream().map(this::codeResponse).toList(); }
    @Transactional
    public ReferralCodeResponse toggle(UUID id, boolean active) {
        ReferralCode code = codes.findById(id).orElseThrow(() -> new NotFoundException("Referral code not found"));
        code.setActive(active); return codeResponse(codes.save(code));
    }
    @Transactional
    public ReferralCodeResponse regenerate(UUID id) {
        ReferralCode code = codes.findById(id).orElseThrow(() -> new NotFoundException("Referral code not found"));
        code.setCode(nextCode()); return codeResponse(codes.save(code));
    }
    @Transactional(readOnly = true)
    public List<ReferralRegistrationResponse> adminReferrals(UUID technicianId) {
        return registrations.findByReferrerTechnicianIdOrderByCreatedAtDesc(technicianId).stream().map(this::registrationResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<ReferralRewardResponse> adminRewards(UUID technicianId) {
        return rewards.findByTechnicianIdOrderByCreatedAtDesc(technicianId).stream().map(this::rewardResponse).toList();
    }

    private String nextCode() {
        String value;
        do {
            StringBuilder suffix = new StringBuilder(6);
            for (int i = 0; i < 6; i++) suffix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            value = "TG-" + suffix;
        } while (codes.existsByCodeIgnoreCase(value));
        return value;
    }
    private ReferralCodeResponse codeResponse(ReferralCode code) {
        List<ReferralRegistration> refs = registrations.findByReferrerTechnicianIdOrderByCreatedAtDesc(code.getTechnician().getId());
        List<ReferralReward> items = rewards.findByTechnicianIdOrderByCreatedAtDesc(code.getTechnician().getId());
        return new ReferralCodeResponse(code.getId(), code.getTechnician().getId(), code.getTechnician().getFullName(),
                code.getCode(), code.isActive(), code.getCreatedAt(), refs.size(),
                refs.stream().filter(item -> item.getStatus() != ReferralRegistrationStatus.REGISTERED && item.getStatus() != ReferralRegistrationStatus.CANCELLED).count(),
                items.stream().filter(item -> item.getStatus() == ReferralRewardStatus.AVAILABLE).count(),
                items.stream().filter(item -> item.getStatus() == ReferralRewardStatus.USED).count());
    }
    private ReferralRegistrationResponse registrationResponse(ReferralRegistration item) {
        return new ReferralRegistrationResponse(item.getId(), item.getReferredUser().getId(),
                item.getReferredUser().getFullName(), item.getReferredUserRole(), item.getStatus(),
                item.getCreatedAt(), item.getQualifiedAt(), item.getRewardGrantedAt());
    }
    private ReferralRewardResponse rewardResponse(ReferralReward item) {
        return new ReferralRewardResponse(item.getId(), item.getRewardType(), item.getStatus(),
                item.getSourceServiceRequest() == null ? null : item.getSourceServiceRequest().getId(),
                item.getUsedServiceRequest() == null ? null : item.getUsedServiceRequest().getId(),
                item.getCreatedAt(), item.getUsedAt(), item.getExpiresAt());
    }
}
