package com.tecngo.content_moderation.entity;

import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_reports")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContentReport {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "content_asset_id")
    private ContentAsset asset;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_id")
    private User reportedBy;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ContentReportStatus status;
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void create() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = ContentReportStatus.OPEN;
    }
}
