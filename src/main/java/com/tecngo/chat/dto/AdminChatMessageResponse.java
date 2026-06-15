package com.tecngo.chat.dto;

import com.tecngo.chat.entity.ChatModerationStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminChatMessageResponse(
        UUID id,
        UUID serviceRequestId,
        UUID senderId,
        String senderName,
        String message,
        ChatModerationStatus moderationStatus,
        String moderationReason,
        long openReports,
        Instant createdAt,
        Instant moderatedAt
) {
}
