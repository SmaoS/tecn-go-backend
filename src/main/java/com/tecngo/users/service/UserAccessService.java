package com.tecngo.users.service;

import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.dto.InactivateUserRequest;
import com.tecngo.users.dto.InactiveUserResponse;
import com.tecngo.users.entity.AccountStatus;
import com.tecngo.users.entity.InactivationReason;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccessService {
    private final UserRepository users;

    public void requireActive(User user) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ConflictException("Your account is inactive: " + user.getAccountStatus());
        }
    }

    @Transactional
    public InactiveUserResponse inactivate(UUID id, InactivateUserRequest request, User admin) {
        requireAdmin(admin);
        User target = requireManagedUser(id);
        if (target.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ConflictException("User is already inactive");
        }
        target.setAccountStatus(statusFor(request.reason()));
        target.setInactiveReason(request.reason());
        target.setInactiveComment(request.comment().trim());
        target.setInactivatedAt(Instant.now());
        target.setInactivatedBy(admin);
        return map(target);
    }

    @Transactional
    public InactiveUserResponse activate(UUID id, User admin) {
        requireAdmin(admin);
        User target = requireManagedUser(id);
        target.setAccountStatus(AccountStatus.ACTIVE);
        target.setInactiveReason(null);
        target.setInactiveComment(null);
        target.setInactivatedAt(null);
        target.setInactivatedBy(null);
        return map(target);
    }

    @Transactional(readOnly = true)
    public List<InactiveUserResponse> inactive(User admin) {
        requireAdmin(admin);
        return users.findByAccountStatusNotOrderByCreatedAtDesc(AccountStatus.ACTIVE)
                .stream().map(this::map).toList();
    }

    private User requireManagedUser(UUID id) {
        User target = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        if (!target.hasRole(Role.CLIENT) && !target.hasRole(Role.TECHNICIAN)) {
            throw new ForbiddenException("Only clients and technicians can be inactivated");
        }
        return target;
    }

    private AccountStatus statusFor(InactivationReason reason) {
        return switch (reason) {
            case PAYMENT_ISSUE -> AccountStatus.INACTIVE_PAYMENT;
            case REPORT -> AccountStatus.INACTIVE_REPORT;
            case SECURITY_RISK -> AccountStatus.BLOCKED;
            case ADMIN_DECISION, OTHER -> AccountStatus.INACTIVE_ADMIN;
        };
    }

    private void requireAdmin(User user) {
        if (user.getRole() != Role.ADMIN) throw new ForbiddenException("Admin role is required");
    }

    private InactiveUserResponse map(User user) {
        return new InactiveUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getAccountStatus(), user.getInactiveReason(), user.getInactiveComment(),
                user.getInactivatedAt(),
                user.getInactivatedBy() == null ? null : user.getInactivatedBy().getId());
    }
}
