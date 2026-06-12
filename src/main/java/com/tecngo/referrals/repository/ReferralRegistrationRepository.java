package com.tecngo.referrals.repository;
import com.tecngo.referrals.entity.ReferralRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ReferralRegistrationRepository extends JpaRepository<ReferralRegistration, UUID> {
    Optional<ReferralRegistration> findByReferredUserId(UUID userId);
    List<ReferralRegistration> findByReferrerTechnicianIdOrderByCreatedAtDesc(UUID technicianId);
    long countByReferrerTechnicianId(UUID technicianId);
}
