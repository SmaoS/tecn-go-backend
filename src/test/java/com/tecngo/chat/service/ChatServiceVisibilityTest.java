package com.tecngo.chat.service;

import com.tecngo.chat.entity.ChatMessage;
import com.tecngo.chat.entity.ChatModerationStatus;
import com.tecngo.users.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ChatServiceVisibilityTest {
    private final ChatService service = new ChatService(
            mock(com.tecngo.service_requests.repository.ServiceRequestRepository.class),
            mock(com.tecngo.chat.repository.ChatRoomRepository.class),
            mock(com.tecngo.chat.repository.ChatMessageRepository.class),
            mock(com.tecngo.chat.repository.ChatModerationAuditRepository.class),
            mock(ChatModerationService.class),
            mock(org.springframework.context.ApplicationEventPublisher.class),
            mock(com.tecngo.users.repository.UserRepository.class));

    @Test
    void blockedMessageTextIsNeverReturnedToParticipant() {
        User sender = User.builder().id(UUID.randomUUID()).fullName("Usuario").build();
        User viewer = User.builder().id(UUID.randomUUID()).build();
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .message("Texto prohibido")
                .moderationStatus(ChatModerationStatus.BLOCKED)
                .moderationReason("Threat")
                .createdAt(Instant.now())
                .build();

        var response = service.map(message, viewer);

        assertThat(response.message()).isEqualTo("Mensaje bloqueado por seguridad");
        assertThat(response.message()).doesNotContain("Texto prohibido");
    }
}
