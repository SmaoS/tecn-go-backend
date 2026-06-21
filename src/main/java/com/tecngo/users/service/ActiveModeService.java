package com.tecngo.users.service;

import com.tecngo.auth.service.JwtService;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.users.dto.ActiveModeResponse;
import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.OnboardingStep;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActiveModeService {
    private final UserRepository users;
    private final ActiveModeAuditService audits;
    private final JwtService jwtService;

    @Transactional
    public ActiveModeResponse change(User authenticatedUser, ActiveMode requestedMode) {
        User user = users.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ForbiddenException("Authenticated user no longer exists"));

        Role requestedRole = requestedMode.asRole();
        boolean roleCreated = !user.hasRole(requestedRole);
        if (roleCreated) {
            user.addRole(requestedRole);
            if (requestedRole == Role.TECHNICIAN) {
                user.setOnboardingCompleted(false);
                user.setOnboardingStep(OnboardingStep.TECHNICIAN_PROFESSIONAL_PROFILE);
            }
        }

        ActiveMode previousMode = user.getActiveMode();
        if (roleCreated || previousMode != requestedMode) {
            user.setActiveMode(requestedMode);
            users.save(user);
            audits.record(user, user, previousMode, requestedMode,
                    roleCreated ? "USER_CAPABILITY_CREATED_AND_MODE_CHANGED" : "USER_MODE_CHANGE");
        }

        return new ActiveModeResponse(
                jwtService.generateToken(user),
                user.getRole(),
                user.getEffectiveRoles(),
                user.getActiveMode(),
                roleCreated,
                user.isOnboardingCompleted(),
                user.getOnboardingStep());
    }
}
