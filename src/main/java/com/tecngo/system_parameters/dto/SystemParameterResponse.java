package com.tecngo.system_parameters.dto;

import com.tecngo.system_parameters.entity.ParameterType;

import java.time.Instant;
import java.util.UUID;

public record SystemParameterResponse(
        UUID id, String key, String value, String description,
        ParameterType type, boolean active, Instant updatedAt
) {}
