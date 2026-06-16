package com.tecngo.users.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileSelfieRequest(@NotBlank String profilePhotoUrl) {}
