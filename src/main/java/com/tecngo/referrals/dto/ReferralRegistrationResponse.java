package com.tecngo.referrals.dto;
import com.tecngo.referrals.entity.ReferralRegistrationStatus;
import com.tecngo.users.entity.Role;
import java.time.Instant;
import java.util.UUID;
public record ReferralRegistrationResponse(UUID id, UUID referredUserId, String referredUserName,
        Role referredUserRole, ReferralRegistrationStatus status, Instant createdAt,
        Instant qualifiedAt, Instant rewardGrantedAt) {}
