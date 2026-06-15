package com.tecngo.chat.service;

import com.tecngo.chat.entity.*;
import com.tecngo.chat.repository.ChatMessageReportRepository;
import com.tecngo.chat.repository.ChatMessageRepository;
import com.tecngo.chat.repository.ChatModerationAuditRepository;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.users.service.UserAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatReviewServiceTest {
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final ChatMessageReportRepository reports = mock(ChatMessageReportRepository.class);
    private final ChatModerationAuditRepository audits = mock(ChatModerationAuditRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final UserAccessService userAccess = mock(UserAccessService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ChatService chatService = mock(ChatService.class);
    private final ChatReviewService service = new ChatReviewService(
            messages, reports, audits, users, userAccess, events, chatService);

    private User client;
    private User technician;
    private ChatMessage message;

    @BeforeEach
    void setUp() {
        client = User.builder().id(UUID.randomUUID()).role(Role.CLIENT).fullName("Cliente").build();
        technician = User.builder().id(UUID.randomUUID()).role(Role.TECHNICIAN).fullName("Tecnico").build();
        ServiceRequest request = ServiceRequest.builder()
                .id(UUID.randomUUID()).client(client).technician(technician).build();
        ChatRoom room = ChatRoom.builder().id(UUID.randomUUID()).serviceRequest(request).build();
        message = ChatMessage.builder()
                .id(UUID.randomUUID())
                .room(room)
                .sender(technician)
                .message("Mensaje original")
                .moderationStatus(ChatModerationStatus.APPROVED)
                .createdAt(Instant.now())
                .build();
        when(messages.findById(message.getId())).thenReturn(Optional.of(message));
        when(users.findByRoleInOrderByCreatedAtDesc(anySet())).thenReturn(List.of(
                User.builder().id(UUID.randomUUID()).role(Role.VERIFIER).build()));
    }

    @Test
    void participantReportFlagsApprovedMessageAndCreatesAudit() {
        when(reports.existsByMessageIdAndReportedByIdAndStatus(
                message.getId(), client.getId(), ChatReportStatus.OPEN)).thenReturn(false);

        service.report(message.getId(), "Amenaza", client);

        assertThat(message.getModerationStatus()).isEqualTo(ChatModerationStatus.FLAGGED);
        verify(reports).save(any(ChatMessageReport.class));
        verify(audits).save(argThat(audit -> audit.getAction() == ChatAuditAction.USER_REPORTED));
        verify(events, atLeastOnce()).publishEvent(any(UserNotificationEvent.class));
    }

    @Test
    void nonParticipantCannotReportMessage() {
        User stranger = User.builder().id(UUID.randomUUID()).role(Role.CLIENT).build();

        assertThatThrownBy(() -> service.report(message.getId(), "Reporte", stranger))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void senderCannotReportOwnMessage() {
        assertThatThrownBy(() -> service.report(message.getId(), "Reporte", technician))
                .isInstanceOf(ForbiddenException.class);
    }
}
