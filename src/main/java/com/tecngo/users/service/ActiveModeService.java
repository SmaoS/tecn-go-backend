package com.tecngo.users.service;

import com.tecngo.auth.service.JwtService;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.users.dto.ActiveModeResponse;
import com.tecngo.users.entity.ActiveMode;
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
        if (!user.hasRole(requestedMode.asRole())) {
            throw new ForbiddenException("Your account does not have the " + requestedMode + " capability");
        }

        ActiveMode previousMode = user.getActiveMode();
        if (previousMode != requestedMode) {
            user.setActiveMode(requestedMode);
            users.save(user);
            audits.record(user, user, previousMode, requestedMode, "USER_MODE_CHANGE");
        }

        return new ActiveModeResponse(
                jwtService.generateToken(user),
                user.getRole(),
                user.getEffectiveRoles(),
                user.getActiveMode());
    }
}
