package com.tecngo.app_versions.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name = "app_versions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AppVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, unique = true) private AppPlatform platform;
    @Column(nullable = false, length = 30) private String minimumSupportedVersion;
    @Column(nullable = false, length = 30) private String latestVersion;
    @Column(nullable = false) private boolean forceUpdate;
    @Column(nullable = false, length = 1000) private String updateUrl;
    @Column(nullable = false, length = 500) private String message;
    @Column(nullable = false) private boolean active;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @PrePersist void create() { Instant now=Instant.now(); if(createdAt==null)createdAt=now; updatedAt=now; }
    @PreUpdate void update(){updatedAt=Instant.now();}
}
