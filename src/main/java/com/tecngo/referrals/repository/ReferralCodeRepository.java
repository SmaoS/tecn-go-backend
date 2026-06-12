package com.tecngo.referrals.repository;
import com.tecngo.referrals.entity.ReferralCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ReferralCodeRepository extends JpaRepository<ReferralCode, UUID> {
    Optional<ReferralCode> findByTechnicianId(UUID technicianId);
    Optional<ReferralCode> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
