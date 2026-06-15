package com.tecngo.service_evidence.entity;

import com.tecngo.content_moderation.entity.ContentAsset;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "service_evidences")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ServiceEvidence {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id") private ServiceRequest serviceRequest;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "uploaded_by_user_id") private User uploadedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role uploadedByRole;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EvidenceType evidenceType;
    @Column(nullable = false, length = 1000) private String fileUrl;
    @Column(nullable = false, length = 500) private String publicId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "content_asset_id") private ContentAsset contentAsset;
    @Column(length = 1000) private String description;
    @Column(nullable = false) private Instant createdAt;
    @PrePersist void create() { if (createdAt == null) createdAt = Instant.now(); }
}
