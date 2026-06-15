package com.tecngo.chat.service;

import com.tecngo.chat.dto.AdminChatMessageResponse;
import com.tecngo.chat.dto.ChatMessageResponse;
import com.tecngo.chat.entity.*;
import com.tecngo.chat.repository.ChatMessageReportRepository;
import com.tecngo.chat.repository.ChatMessageRepository;
import com.tecngo.chat.repository.ChatModerationAuditRepository;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.dto.InactivateUserRequest;
import com.tecngo.users.entity.InactivationReason;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.users.service.UserAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatReviewService {
    private final ChatMessageRepository messages;
    private final ChatMessageReportRepository reports;
    private final ChatModerationAuditRepository audits;
    private final UserRepository users;
    private final UserAccessService userAccess;
    private final ApplicationEventPublisher events;
    private final ChatService chatService;

    @Transactional
    public ChatMessageResponse report(UUID messageId, String reason, User reporter) {
        ChatMessage message = requireMessage(messageId);
        requireParticipant(message, reporter);
        if (message.getSender().getId().equals(reporter.getId())) {
            throw new ForbiddenException("Users cannot report their own message");
        }
        boolean newReport = !reports.existsByMessageIdAndReportedByIdAndStatus(
                messageId, reporter.getId(), ChatReportStatus.OPEN);
        if (newReport) {
            reports.save(ChatMessageReport.builder()
                    .message(message)
                    .reportedBy(reporter)
                    .reason(reason.trim())
                    .build());
        }
        ChatModerationStatus previous = message.getModerationStatus();
        if (previous == ChatModerationStatus.APPROVED) {
            message.setModerationStatus(ChatModerationStatus.FLAGGED);
            message.setModerationReason("Reported by participant: " + reason.trim());
            message.setModeratedAt(null);
            message.setModeratedBy(null);
        }
        if (newReport) {
            audit(message, ChatAuditAction.USER_REPORTED, previous,
                    message.getModerationStatus(), reason, reporter);
            notifyReviewers(message);
        }
        return chatService.map(message, reporter);
    }

    @Transactional(readOnly = true)
    public List<AdminChatMessageResponse> queue(ChatModerationStatus status, User reviewer) {
        requireReviewer(reviewer);
        if (status == ChatModerationStatus.APPROVED) {
            throw new ForbiddenException("Approved private conversations cannot be browsed");
        }
        List<ChatModerationStatus> statuses = status == null
                ? List.of(ChatModerationStatus.FLAGGED, ChatModerationStatus.BLOCKED)
                : List.of(status);
        return messages.findByModerationStatusInOrderByCreatedAtDesc(statuses).stream()
                .map(this::mapAdmin).toList();
    }

    @Transactional
    public AdminChatMessageResponse decide(UUID id, ChatModerationStatus status,
                                           String reason, User reviewer) {
        requireReviewer(reviewer);
        if (status != ChatModerationStatus.APPROVED && status != ChatModerationStatus.BLOCKED) {
            throw new IllegalArgumentException("Manual moderation must approve or block a message");
        }
        ChatMessage message = requireMessage(id);
        ChatModerationStatus previous = message.getModerationStatus();
        message.setModerationStatus(status);
        message.setModerationReason(clean(reason));
        message.setModeratedAt(Instant.now());
        message.setModeratedBy(reviewer);
        resolveReports(id, reviewer);
        audit(message,
                status == ChatModerationStatus.APPROVED
                        ? ChatAuditAction.MANUAL_APPROVED : ChatAuditAction.MANUAL_BLOCKED,
                previous, status, reason, reviewer);
        return mapAdmin(message);
    }

    @Transactional
    public AdminChatMessageResponse sanction(UUID id, String reason, User admin) {
        if (admin.getRole() != Role.ADMIN) throw new ForbiddenException("Admin role is required");
        ChatMessage message = requireMessage(id);
        userAccess.inactivate(message.getSender().getId(),
                new InactivateUserRequest(InactivationReason.SECURITY_RISK, reason.trim()), admin);
        ChatModerationStatus previous = message.getModerationStatus();
        message.setModerationStatus(ChatModerationStatus.BLOCKED);
        message.setModerationReason(reason.trim());
        message.setModeratedAt(Instant.now());
        message.setModeratedBy(admin);
        resolveReports(id, admin);
        audit(message, ChatAuditAction.USER_SANCTIONED, previous,
                ChatModerationStatus.BLOCKED, reason, admin);
        return mapAdmin(message);
    }

    private void notifyReviewers(ChatMessage message) {
        users.findByRoleInOrderByCreatedAtDesc(Set.of(Role.ADMIN, Role.VERIFIER)).forEach(user ->
                events.publishEvent(new UserNotificationEvent(
                        user.getId(),
                        "Mensaje reportado",
                        "Un mensaje de chat requiere revisión",
                        NotificationType.CHAT_MODERATION_ALERT,
                        Map.of("type", "CHAT_MODERATION", "route", "AdminOperations"))));
    }

    private void audit(ChatMessage message, ChatAuditAction action,
                       ChatModerationStatus previous, ChatModerationStatus next,
                       String reason, User actor) {
        audits.save(ChatModerationAudit.builder()
                .message(message)
                .action(action)
                .previousStatus(previous)
                .newStatus(next)
                .reason(clean(reason))
                .actor(actor)
                .build());
    }

    private void resolveReports(UUID messageId, User reviewer) {
        reports.findByMessageIdAndStatus(messageId, ChatReportStatus.OPEN).forEach(report -> {
            report.setStatus(ChatReportStatus.RESOLVED);
            report.setResolvedAt(Instant.now());
            report.setResolvedBy(reviewer);
        });
    }

    private AdminChatMessageResponse mapAdmin(ChatMessage message) {
        return new AdminChatMessageResponse(
                message.getId(),
                message.getRoom().getServiceRequest().getId(),
                message.getSender().getId(),
                message.getSender().getFullName(),
                message.getMessage(),
                message.getModerationStatus(),
                message.getModerationReason(),
                reports.countByMessageIdAndStatus(message.getId(), ChatReportStatus.OPEN),
                message.getCreatedAt(),
                message.getModeratedAt());
    }

    private void requireParticipant(ChatMessage message, User user) {
        var request = message.getRoom().getServiceRequest();
        boolean participant = request.getClient().getId().equals(user.getId())
                || request.getTechnician() != null
                && request.getTechnician().getId().equals(user.getId());
        if (!participant) throw new ForbiddenException("Only service participants can report chat messages");
    }

    private ChatMessage requireMessage(UUID id) {
        return messages.findById(id)
                .orElseThrow(() -> new NotFoundException("Chat message not found"));
    }

    private void requireReviewer(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.VERIFIER) {
            throw new ForbiddenException("Admin or verifier role is required");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
