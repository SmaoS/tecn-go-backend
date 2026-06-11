package com.tecngo.admin.dto;

public record AdminDashboardResponse(
        long users,
        long pendingTechnicians,
        long pendingVerifications,
        long payments
) {}
