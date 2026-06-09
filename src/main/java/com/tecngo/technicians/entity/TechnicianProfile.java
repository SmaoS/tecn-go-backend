package com.tecngo.technicians.entity;

import com.tecngo.users.entity.User;
import com.tecngo.services.entity.ServiceCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "technician_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true)
    private String documentNumber;

    @Column(nullable = false)
    private String phone;

    @ManyToMany
    @JoinTable(name = "technician_profile_categories",
            joinColumns = @JoinColumn(name = "technician_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @Builder.Default
    private Set<ServiceCategory> categories = new HashSet<>();

    @Column(nullable = false, length = 1000)
    private String description;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TechnicianStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (status == null) status = TechnicianStatus.PENDING;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
