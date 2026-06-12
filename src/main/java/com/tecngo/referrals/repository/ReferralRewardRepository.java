package com.tecngo.referrals.repository;
import com.tecngo.referrals.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface ReferralRewardRepository extends JpaRepository<ReferralReward, UUID> {
    List<ReferralReward> findByTechnicianIdOrderByCreatedAtDesc(UUID technicianId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReferralReward r where r.technician.id=:technicianId and r.status='AVAILABLE' and (r.expiresAt is null or r.expiresAt > CURRENT_TIMESTAMP) order by r.createdAt")
    List<ReferralReward> findAvailableForUpdate(@Param("technicianId") UUID technicianId);
}
