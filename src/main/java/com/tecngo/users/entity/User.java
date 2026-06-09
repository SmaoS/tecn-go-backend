package com.tecngo.users.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 500)
    private String fcmToken;

    @Column(length = 500)
    private String profilePhotoUrl;

    @Column(length = 500)
    private String documentPhotoUrl;

    @Column(length = 500)
    private String certificatePhotoUrl;

    @Column(length = 1000)
    private String workExperienceDescription;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(nullable = false)
    private long completedServicesCount;

    @Column(nullable = false)
    private long paidServicesCount;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (averageRating == null) averageRating = new BigDecimal("5.00");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }
}
