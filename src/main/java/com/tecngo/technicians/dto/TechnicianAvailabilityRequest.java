package com.tecngo.technicians.dto;

import jakarta.validation.constraints.NotNull;

public record TechnicianAvailabilityRequest(@NotNull Boolean available) {
}
