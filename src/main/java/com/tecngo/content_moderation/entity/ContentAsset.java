package com.tecngo.content_moderation.entity;

import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_assets")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContentAsset {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ContentAssetKind kind;
    @Column(nullable = false, unique = true, length = 1000)
    private String fileUrl;
    @Column(nullable = false, length = 1000)
    private String secureUrl;
    @Column(nullable = false, length = 500)
    private String publicId;
    @Column(nullable = false, length = 120)
    private String contentType;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ModerationStatus moderationStatus;
    @Column(length = 1000)
    private String moderationReason;
    private Instant moderatedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by_user_id")
    private User moderatedBy;
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void create() {
        if (createdAt == null) createdAt = Instant.now();
        if (moderationStatus == null) moderationStatus = ModerationStatus.PENDING_REVIEW;
    }
}
