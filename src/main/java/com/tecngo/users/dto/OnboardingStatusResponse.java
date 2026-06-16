package com.tecngo.users.dto;

import com.tecngo.users.entity.OnboardingStep;

import java.util.List;

public record OnboardingStatusResponse(
        boolean emailVerified,
        boolean onboardingCompleted,
        OnboardingStep currentStep,
        List<OnboardingStep> requiredSteps,
        String nextScreen
) {}
