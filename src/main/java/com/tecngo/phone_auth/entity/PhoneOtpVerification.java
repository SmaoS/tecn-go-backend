package com.tecngo.phone_auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phone_otp_verifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneOtpVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 100)
    private String codeHash;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(length = 255)
    private String providerReference;

    @Column(nullable = false, length = 64)
    private String requestIpHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant verifiedAt;

    @Column(length = 64)
    private String verificationTokenHash;

    private Instant consumedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
