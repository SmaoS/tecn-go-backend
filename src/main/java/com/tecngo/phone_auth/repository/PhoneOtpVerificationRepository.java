package com.tecngo.phone_auth.repository;

import com.tecngo.phone_auth.entity.PhoneOtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PhoneOtpVerificationRepository extends JpaRepository<PhoneOtpVerification, UUID> {
    Optional<PhoneOtpVerification> findFirstByPhoneAndVerifiedFalseOrderByCreatedAtDesc(String phone);
    Optional<PhoneOtpVerification> findByPhoneAndVerificationTokenHashAndVerifiedTrueAndConsumedAtIsNull(
            String phone, String verificationTokenHash);
    long countByPhoneAndCreatedAtAfter(String phone, Instant after);
    long countByRequestIpHashAndCreatedAtAfter(String requestIpHash, Instant after);
}
