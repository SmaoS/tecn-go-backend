package com.tecngo.users.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_selfie_change_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSelfieChangeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 500)
    private String currentPhotoUrl;

    @Column(nullable = false, length = 500)
    private String requestedPhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private FaceDetectionStatus faceDetectionStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProfileSelfieChangeRequestStatus status;

    @Column(nullable = false)
    private Instant requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    private Instant reviewedAt;

    @Column(length = 1000)
    private String rejectionReason;

    @PrePersist
    void create() {
        if (requestedAt == null) requestedAt = Instant.now();
        if (status == null) status = ProfileSelfieChangeRequestStatus.PENDING;
    }
}
