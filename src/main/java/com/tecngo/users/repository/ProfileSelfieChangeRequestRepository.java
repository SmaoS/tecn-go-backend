package com.tecngo.users.repository;

import com.tecngo.users.entity.ProfileSelfieChangeRequest;
import com.tecngo.users.entity.ProfileSelfieChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfileSelfieChangeRequestRepository extends JpaRepository<ProfileSelfieChangeRequest, UUID> {
    boolean existsByUserIdAndStatus(UUID userId, ProfileSelfieChangeRequestStatus status);
    List<ProfileSelfieChangeRequest> findByUserIdOrderByRequestedAtDesc(UUID userId);
    List<ProfileSelfieChangeRequest> findByStatusOrderByRequestedAtAsc(ProfileSelfieChangeRequestStatus status);
}
