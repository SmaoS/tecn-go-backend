package com.tecngo.chat.repository;

import com.tecngo.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(UUID roomId);

    @Modifying
    @Query("""
            update ChatMessage message
            set message.readAt = :readAt
            where message.room.id = :roomId
              and message.sender.id <> :readerId
              and message.readAt is null
            """)
    int markRead(@Param("roomId") UUID roomId, @Param("readerId") UUID readerId,
                 @Param("readAt") Instant readAt);
}
