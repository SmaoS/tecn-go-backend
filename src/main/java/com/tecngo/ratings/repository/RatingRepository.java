package com.tecngo.ratings.repository;

import com.tecngo.ratings.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    boolean existsByServiceRequestIdAndRaterId(UUID serviceRequestId, UUID raterId);
    List<Rating> findByRatedUserIdOrderByCreatedAtDesc(UUID ratedUserId);
}
