package com.tecngo.compliance.dto;

import jakarta.validation.constraints.Size;

public record AnonymizationRequest(@Size(max = 1000) String reason) {}
