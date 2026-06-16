package com.tecngo.service_requests.entity;

import com.tecngo.catalogs.entity.City;
import com.tecngo.payments.entity.PaymentMethod;
import com.tecngo.services.entity.ServiceCategory;
import com.tecngo.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "service_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User client;
    @ManyToOne(fetch = FetchType.LAZY)
    private User technician;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ServiceCategory category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;
    @Column(nullable = false, length = 1000)
    private String description;
    @Column(nullable = false)
    private String address;
    private Double latitude;
    private Double longitude;
    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedPrice;
    @Column(precision = 12, scale = 2)
    private BigDecimal technicianPrice;
    @Column(precision = 12, scale = 2)
    private BigDecimal finalPrice;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod requestedPaymentMethod;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (status == null) status = RequestStatus.QUOTE_PENDING;
        if (createdAt == null) createdAt = Instant.now();
    }
}
