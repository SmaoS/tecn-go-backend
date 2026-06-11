package com.tecngo.system_parameters.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSystemParameterRequest(@NotBlank String value) {}
