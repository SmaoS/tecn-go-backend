package com.tecngo.chat.service;

import com.tecngo.chat.dto.ChatMessageResponse;
import com.tecngo.chat.entity.ChatAuditAction;
import com.tecngo.chat.entity.ChatMessage;
import com.tecngo.chat.entity.ChatModerationAudit;
import com.tecngo.chat.entity.ChatModerationStatus;
import com.tecngo.chat.entity.ChatRoom;
import com.tecngo.chat.repository.ChatModerationAuditRepository;
import com.tecngo.chat.repository.ChatMessageRepository;
import com.tecngo.chat.repository.ChatRoomRepository;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.Role;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ServiceRequestRepository requests;
    private final ChatRoomRepository rooms;
    private final ChatMessageRepository messages;
    private final ChatModerationAuditRepository audits;
    private final ChatModerationService moderation;
    private final ApplicationEventPublisher events;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(UUID requestId, User user) {
        ServiceRequest request = participantRequest(requestId, user);
        return rooms.findByServiceRequestId(request.getId())
                .map(room -> messages.findByRoomIdOrderByCreatedAtAsc(room.getId()).stream()
                        .map(item -> map(item, user)).toList())
                .orElseGet(List::of);
    }

    @Transactional
    public ChatMessageResponse send(UUID requestId, String text, User sender) {
        ServiceRequest request = participantRequest(requestId, sender);
        ChatRoom room = rooms.findByServiceRequestId(requestId)
                .orElseGet(() -> rooms.save(ChatRoom.builder().serviceRequest(request).build()));
        ChatModerationService.ModerationResult result = moderation.moderate(text.trim());
        ChatMessage message = messages.save(ChatMessage.builder()
                .room(room)
                .sender(sender)
                .message(text.trim())
                .moderationStatus(result.status())
                .moderationReason(result.reason())
                .moderatedAt(Instant.now())
                .build());
        audits.save(ChatModerationAudit.builder()
                .message(message)
                .action(auditAction(result.status()))
                .previousStatus(ChatModerationStatus.PENDING)
                .newStatus(result.status())
                .reason(result.reason())
                .build());
        if (result.status() != ChatModerationStatus.APPROVED) {
            users.findByRoleInOrderByCreatedAtDesc(Set.of(Role.ADMIN, Role.VERIFIER)).forEach(user ->
                    events.publishEvent(new UserNotificationEvent(
                            user.getId(),
                            "Mensaje pendiente de moderación",
                            "Un mensaje de chat fue " + result.status().name().toLowerCase(),
                            NotificationType.CHAT_MODERATION_ALERT,
                            Map.of("type", "CHAT_MODERATION", "route", "AdminOperations"))));
        }
        User recipient = request.getClient().getId().equals(sender.getId())
                ? request.getTechnician() : request.getClient();
        if (result.status() != ChatModerationStatus.BLOCKED) {
            String notificationText = result.status() == ChatModerationStatus.APPROVED
                    ? sender.getFullName() + ": " + text.trim()
                    : sender.getFullName() + " envió un mensaje pendiente de revisión";
            events.publishEvent(new UserNotificationEvent(recipient.getId(), "Nuevo mensaje",
                    notificationText, NotificationType.NEW_CHAT_MESSAGE,
                    Map.of(
                            "type", "CHAT",
                            "requestId", request.getId().toString(),
                            "route", "Chat")));
        }
        return map(message, sender);
    }

    @Transactional
    public int markRead(UUID requestId, User user) {
        ServiceRequest request = participantRequest(requestId, user);
        return rooms.findByServiceRequestId(request.getId())
                .map(room -> messages.markRead(room.getId(), user.getId(), Instant.now()))
                .orElse(0);
    }

    private ServiceRequest participantRequest(UUID id, User user) {
        ServiceRequest request = requests.findById(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        boolean client = request.getClient().getId().equals(user.getId());
        boolean technician = request.getTechnician() != null
                && request.getTechnician().getId().equals(user.getId());
        if (!client && !technician) throw new ForbiddenException("Only service participants can access chat");
        if (request.getTechnician() == null) throw new ForbiddenException("Chat requires an assigned technician");
        return request;
    }

    ChatMessageResponse map(ChatMessage item, User viewer) {
        String visibleMessage = item.getMessage();
        if (item.getModerationStatus() == ChatModerationStatus.BLOCKED) {
            visibleMessage = "Mensaje bloqueado por seguridad";
        } else if (item.getModerationStatus() == ChatModerationStatus.FLAGGED
                && !item.getSender().getId().equals(viewer.getId())) {
            visibleMessage = "Mensaje enviado a revisión";
        }
        return new ChatMessageResponse(item.getId(), item.getSender().getId(),
                item.getSender().getFullName(), visibleMessage, item.getModerationStatus(),
                item.getModerationReason(), item.getCreatedAt(), item.getReadAt());
    }

    private ChatAuditAction auditAction(ChatModerationStatus status) {
        return switch (status) {
            case APPROVED -> ChatAuditAction.AUTO_APPROVED;
            case FLAGGED -> ChatAuditAction.AUTO_FLAGGED;
            case BLOCKED -> ChatAuditAction.AUTO_BLOCKED;
            case PENDING -> throw new IllegalArgumentException("Pending is not a final moderation result");
        };
    }
}
