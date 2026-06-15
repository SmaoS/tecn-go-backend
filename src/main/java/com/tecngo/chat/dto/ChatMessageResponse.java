package com.tecngo.chat.dto;

import com.tecngo.chat.entity.ChatModerationStatus;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id, UUID senderId, String senderName, String message,
        ChatModerationStatus moderationStatus, String moderationReason,
        Instant createdAt, Instant readAt
) {}
