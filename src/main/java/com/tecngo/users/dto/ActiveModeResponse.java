package com.tecngo.users.dto;

import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.OnboardingStep;
import com.tecngo.users.entity.Role;

import java.util.Set;

public record ActiveModeResponse(
        String token,
        Role role,
        Set<Role> roles,
        ActiveMode activeMode,
        boolean roleCreated,
        boolean onboardingCompleted,
        OnboardingStep onboardingStep
) {
}
