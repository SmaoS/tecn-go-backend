package com.tecngo.referrals.dto;
import com.tecngo.referrals.entity.*;
import java.time.Instant;
import java.util.UUID;
public record ReferralRewardResponse(UUID id, ReferralRewardType rewardType, ReferralRewardStatus status,
        UUID sourceServiceRequestId, UUID usedServiceRequestId, Instant createdAt, Instant usedAt, Instant expiresAt) {}
