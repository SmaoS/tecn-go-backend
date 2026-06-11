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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private boolean phoneVerified;

    @Column(nullable = false)
    private boolean documentsVerified;

    @Column(length = 30)
    private String phone;

    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 500)
    private String fcmToken;

    private Instant fcmTokenUpdatedAt;

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

    @Column(length = 255)
    private String homeAddress;

    private Double homeLatitude;
    private Double homeLongitude;

    @Column(length = 120)
    private String homeCity;

    @Column(length = 120)
    private String homeNeighborhood;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus;

    @Enumerated(EnumType.STRING)
    private InactivationReason inactiveReason;

    @Column(length = 1000)
    private String inactiveComment;
    private Instant inactivatedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inactivated_by_user_id")
    private User inactivatedBy;

    @Column(length = 500)
    private String profilePhotoPublicId;
    @Column(nullable = false)
    private boolean profilePhotoFaceValidated;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_photo_verified_by_user_id")
    private User profilePhotoVerifiedBy;
    private Instant profilePhotoVerifiedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (averageRating == null) averageRating = new BigDecimal("5.00");
        if (verificationStatus == null) verificationStatus = VerificationStatus.CREATED;
        if (accountStatus == null) accountStatus = AccountStatus.ACTIVE;
        if (role == Role.ADMIN || role == Role.VERIFIER) emailVerified = true;
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
