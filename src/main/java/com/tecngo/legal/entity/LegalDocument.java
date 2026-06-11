package com.tecngo.legal.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name = "legal_documents")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LegalDocument {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable=false, length=80) private String code;
    @Column(nullable=false) private String title;
    @Column(nullable=false, length=40) private String version;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private LegalRoleTarget roleTarget;
    @Column(nullable=false, columnDefinition="text") private String content;
    @Column(nullable=false) private boolean active;
    @Column(nullable=false) private Instant createdAt;
    @PrePersist void create() { if (createdAt == null) createdAt = Instant.now(); }
}
