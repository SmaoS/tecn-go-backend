package com.tecngo.chat.service;

import com.tecngo.chat.dto.ChatMessageResponse;
import com.tecngo.chat.entity.ChatMessage;
import com.tecngo.chat.entity.ChatRoom;
import com.tecngo.chat.repository.ChatMessageRepository;
import com.tecngo.chat.repository.ChatRoomRepository;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ServiceRequestRepository requests;
    private final ChatRoomRepository rooms;
    private final ChatMessageRepository messages;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(UUID requestId, User user) {
        ServiceRequest request = participantRequest(requestId, user);
        return rooms.findByServiceRequestId(request.getId())
                .map(room -> messages.findByRoomIdOrderByCreatedAtAsc(room.getId()).stream()
                        .map(this::map).toList())
                .orElseGet(List::of);
    }

    @Transactional
    public ChatMessageResponse send(UUID requestId, String text, User sender) {
        ServiceRequest request = participantRequest(requestId, sender);
        ChatRoom room = rooms.findByServiceRequestId(requestId)
                .orElseGet(() -> rooms.save(ChatRoom.builder().serviceRequest(request).build()));
        ChatMessage message = messages.save(ChatMessage.builder()
                .room(room)
                .sender(sender)
                .message(text.trim())
                .build());
        User recipient = request.getClient().getId().equals(sender.getId())
                ? request.getTechnician() : request.getClient();
        events.publishEvent(new UserNotificationEvent(recipient.getId(), "Nuevo mensaje",
                sender.getFullName() + ": " + text.trim(), NotificationType.NEW_CHAT_MESSAGE,
                Map.of(
                        "type", "CHAT",
                        "requestId", request.getId().toString(),
                        "route", "Chat")));
        return map(message);
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

    private ChatMessageResponse map(ChatMessage item) {
        return new ChatMessageResponse(item.getId(), item.getSender().getId(),
                item.getSender().getFullName(), item.getMessage(), item.getCreatedAt(), item.getReadAt());
    }
}
