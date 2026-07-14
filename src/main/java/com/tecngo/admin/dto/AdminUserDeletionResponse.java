package com.tecngo.admin.dto;

import java.util.UUID;

public record AdminUserDeletionResponse(
        UUID userId,
        String email,
        String message
) {}
