package com.tecngo.service_requests.service;

import com.tecngo.legal.service.LegalService;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.UserAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceRequestAccessPolicy {
    private final UserAccessService userAccess;
    private final LegalService legal;

    public void requireRole(User user, Role role) {
        if (!user.hasRole(role)) {
            throw new ForbiddenException("Role " + role + " is required");
        }
        if ((role == Role.CLIENT || role == Role.TECHNICIAN) && !user.isActiveAs(role)) {
            throw new ForbiddenException("Active mode " + role + " is required");
        }
    }

    public void requireCriticalAccess(User user) {
        userAccess.requireActive(user);
        legal.requireAccepted(user);
    }

    public void requireClientOwner(ServiceRequest request, User user) {
        if (!request.getClient().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the client owner can modify this request");
        }
    }

    public void requireAssignedTechnician(ServiceRequest request, User user) {
        if (request.getTechnician() == null || !request.getTechnician().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the assigned technician can update this request");
        }
    }

    public void requireDifferentUser(User first, User second, String message) {
        if (first.getId().equals(second.getId())) {
            throw new ForbiddenException(message);
        }
    }

    public void requireParticipant(ServiceRequest request, User user) {
        boolean participant = request.getClient().getId().equals(user.getId())
                || request.getTechnician() != null
                && request.getTechnician().getId().equals(user.getId());
        if (!participant) {
            throw new ForbiddenException("Only service participants can view this request");
        }
    }
}
