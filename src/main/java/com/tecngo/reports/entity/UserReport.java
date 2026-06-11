package com.tecngo.reports.entity;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.users.entity.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name = "user_reports")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserReport {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id") private ServiceRequest serviceRequest;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "reporter_user_id") private User reporter;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "reported_user_id") private User reported;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role reporterRole;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role reportedRole;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReportReason reason;
    @Column(nullable = false, length = 2000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReportStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReportSeverity severity;
    @Column(nullable = false) private Instant createdAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by_user_id") private User reviewedBy;
    private Instant reviewedAt;
    @Column(length = 2000) private String resolutionComment;
    @PrePersist void create() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = ReportStatus.OPEN;
        if (severity == null) severity = ReportSeverity.MEDIUM;
    }
}
