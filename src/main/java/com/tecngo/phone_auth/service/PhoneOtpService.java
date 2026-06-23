package com.tecngo.phone_auth.service;

import com.tecngo.phone_auth.dto.SendPhoneOtpResponse;
import com.tecngo.phone_auth.dto.VerifyPhoneOtpResponse;
import com.tecngo.phone_auth.entity.PhoneOtpVerification;
import com.tecngo.phone_auth.provider.SmsOtpProvider;
import com.tecngo.phone_auth.repository.PhoneOtpVerificationRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.TooManyRequestsException;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneOtpService {
    private final PhoneOtpVerificationRepository verifications;
    private final UserRepository users;
    private final SmsOtpProvider provider;
    private final PhoneNormalizer phones;
    private final SystemParameterService parameters;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public SendPhoneOtpResponse send(String rawPhone, UUID countryId, String requestIp) {
        String phone = phones.international(rawPhone, countryId);
        String ipHash = hash(requestIp == null ? "unknown" : requestIp);
        Instant window = Instant.now().minus(10, ChronoUnit.MINUTES);
        if (verifications.countByPhoneAndCreatedAtAfter(phone, window) >= parameters.otpMaxSendsPerPhone()
                || verifications.countByRequestIpHashAndCreatedAtAfter(ipHash, window)
                >= parameters.otpMaxSendsPerIp()) {
            throw new TooManyRequestsException("Too many OTP requests. Try again later.");
        }

        int expirationMinutes = parameters.otpExpirationMinutes();
        var dispatch = provider.send(phone, parameters.otpLength());
        Instant expiresAt = Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
        verifications.save(PhoneOtpVerification.builder()
                .phone(phone)
                .codeHash(dispatch.codeForHash())
                .provider(provider.name())
                .providerReference(dispatch.providerReference())
                .requestIpHash(ipHash)
                .expiresAt(expiresAt)
                .build());
        return new SendPhoneOtpResponse(
                "If the phone can receive messages, a verification code was sent.",
                expiresAt,
                dispatch.debugCode());
    }

    @Transactional(noRollbackFor = ConflictException.class)
    public VerifyPhoneOtpResponse verify(String rawPhone, UUID countryId, String code) {
        String phone = phones.international(rawPhone, countryId);
        PhoneOtpVerification verification = verifications
                .findFirstByPhoneAndVerifiedFalseOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new ConflictException("Verification code is invalid or expired"));
        if (verification.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("Verification code is invalid or expired");
        }
        if (verification.getAttempts() >= parameters.otpMaxAttempts()) {
            throw new ConflictException("Maximum verification attempts reached");
        }
        verification.setAttempts(verification.getAttempts() + 1);
        if (!provider.verify(phone, code.trim(), verification.getProviderReference(),
                verification.getCodeHash())) {
            verifications.save(verification);
            throw new ConflictException("Verification code is invalid or expired");
        }

        String rawToken = token();
        verification.setVerified(true);
        verification.setVerifiedAt(Instant.now());
        verification.setExpiresAt(Instant.now().plus(parameters.otpExpirationMinutes(), ChronoUnit.MINUTES));
        verification.setVerificationTokenHash(hash(rawToken));
        users.findByPhoneNormalized(phone).ifPresent(user -> {
            user.setPhone(phones.local(rawPhone));
            user.setPhoneNormalized(phone);
            user.setPhoneVerified(true);
            users.save(user);
        });
        return new VerifyPhoneOtpResponse(true, rawToken);
    }

    @Transactional
    public VerifiedPhone consume(String rawPhone, UUID countryId, String rawToken) {
        String localPhone = phones.local(rawPhone);
        String phone = phones.international(rawPhone, countryId);
        PhoneOtpVerification verification = verifications
                .findByPhoneAndVerificationTokenHashAndVerifiedTrueAndConsumedAtIsNull(phone, hash(rawToken))
                .orElseThrow(() -> new ConflictException("Phone verification is required"));
        if (verification.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("Phone verification expired");
        }
        verification.setConsumedAt(Instant.now());
        return new VerifiedPhone(localPhone, phone);
    }

    public record VerifiedPhone(String local, String international) {}

    private String token() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash OTP value", exception);
        }
    }
}
