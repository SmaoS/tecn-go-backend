package com.tecngo.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record VerifierResponse(UUID id, String fullName, String email, Instant createdAt) {}
