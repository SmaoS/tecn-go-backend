package com.tecngo.referrals.entity;

import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referral_registrations")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReferralRegistration {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "referral_code_id") private ReferralCode referralCode;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "referrer_technician_id") private User referrerTechnician;
    @OneToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "referred_user_id", unique = true) private User referredUser;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role referredUserRole;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReferralRegistrationStatus status;
    @Column(nullable = false) private Instant createdAt;
    private Instant qualifiedAt;
    private Instant rewardGrantedAt;
    @PrePersist void create() { if (createdAt == null) createdAt = Instant.now(); if (status == null) status = ReferralRegistrationStatus.REGISTERED; }
}
