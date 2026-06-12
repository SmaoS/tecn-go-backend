package com.tecngo.referrals.dto;
import java.time.Instant;
import java.util.UUID;
public record ReferralCodeResponse(UUID id, UUID technicianId, String technicianName, String code,
                                   boolean active, Instant createdAt, long registered, long qualified,
                                   long availableRewards, long usedRewards) {}
