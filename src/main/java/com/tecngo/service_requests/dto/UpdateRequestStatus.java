package com.tecngo.service_requests.dto;

import com.tecngo.service_requests.entity.RequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRequestStatus(@NotNull RequestStatus status) {}
