package com.tecngo.legal.entity;
import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name = "legal_acceptances")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LegalAcceptance {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private User user;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="legal_document_id") private LegalDocument legalDocument;
    @Column(nullable=false) private Instant acceptedAt;
    @Column(length=100) private String ipAddress;
    @Column(length=500) private String userAgent;
    @PrePersist void create() { if (acceptedAt == null) acceptedAt = Instant.now(); }
}
