package com.tecngo.users.dto;

import com.tecngo.users.entity.ActiveMode;
import jakarta.validation.constraints.NotNull;

public record ChangeActiveModeRequest(@NotNull ActiveMode mode) {
}
