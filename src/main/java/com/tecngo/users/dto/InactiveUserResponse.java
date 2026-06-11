package com.tecngo.users.dto;

import com.tecngo.users.entity.AccountStatus;
import com.tecngo.users.entity.InactivationReason;
import com.tecngo.users.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record InactiveUserResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        AccountStatus accountStatus,
        InactivationReason reason,
        String comment,
        Instant inactivatedAt,
        UUID inactivatedByUserId
) {
}
