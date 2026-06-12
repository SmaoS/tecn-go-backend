package com.tecngo.referrals.entity;

import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referral_rewards")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReferralReward {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "technician_id") private User technician;
    @OneToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "referral_registration_id", unique = true) private ReferralRegistration referralRegistration;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReferralRewardType rewardType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReferralRewardStatus status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_service_request_id") private ServiceRequest sourceServiceRequest;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "used_service_request_id") private ServiceRequest usedServiceRequest;
    @Column(nullable = false) private Instant createdAt;
    private Instant usedAt;
    private Instant expiresAt;
    @PrePersist void create() { if (createdAt == null) createdAt = Instant.now(); if (status == null) status = ReferralRewardStatus.AVAILABLE; if (rewardType == null) rewardType = ReferralRewardType.FREE_COMMISSION_SERVICE; }
}
