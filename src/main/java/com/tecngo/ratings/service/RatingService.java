package com.tecngo.ratings.service;

import com.tecngo.ratings.dto.CreateRatingRequest;
import com.tecngo.ratings.dto.RatingResponse;
import com.tecngo.ratings.dto.TechnicianRatingSummary;
import com.tecngo.ratings.entity.Rating;
import com.tecngo.ratings.repository.RatingRepository;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratings;
    private final ServiceRequestRepository requests;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    @Transactional
    public RatingResponse create(UUID requestId, CreateRatingRequest input, User rater) {
        ServiceRequest request = requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        if (request.getStatus() != RequestStatus.PAID) {
            throw new ConflictException("Only paid services can be rated");
        }
        if (request.getTechnician() == null) throw new ConflictException("Service request has no technician");
        User ratedUser;
        if (rater.getRole() == Role.CLIENT && request.getClient().getId().equals(rater.getId())) {
            ratedUser = request.getTechnician();
        } else if (rater.getRole() == Role.TECHNICIAN
                && request.getTechnician().getId().equals(rater.getId())) {
            ratedUser = request.getClient();
        } else {
            throw new ForbiddenException("Only service participants can rate each other");
        }
        if (ratings.existsByServiceRequestIdAndRaterId(requestId, rater.getId())) {
            throw new ConflictException("You already rated this service");
        }
        Rating saved = ratings.save(Rating.builder()
                .serviceRequest(request)
                .rater(rater)
                .ratedUser(ratedUser)
                .score(input.score())
                .comment(input.comment() == null ? null : input.comment().trim())
                .build());
        recalculate(ratedUser);
        events.publishEvent(new UserNotificationEvent(
                ratedUser.getId(),
                "Nueva calificación",
                rater.getFullName() + " calificó el servicio con " + input.score() + " estrellas",
                NotificationType.NEW_RATING,
                Map.of(
                        "type", "RATING",
                        "requestId", request.getId().toString(),
                        "route", "Rating")));
        return map(saved);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> technicianRatings(UUID technicianId) {
        requireTechnician(technicianId);
        return ratings.findByRatedUserIdOrderByCreatedAtDesc(technicianId).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public TechnicianRatingSummary technicianSummary(UUID technicianId) {
        User technician = requireTechnician(technicianId);
        List<Rating> items = ratings.findByRatedUserIdOrderByCreatedAtDesc(technicianId);
        return new TechnicianRatingSummary(technicianId, technician.getFullName(),
                technician.getAverageRating(), items.size());
    }

    private User requireTechnician(UUID id) {
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("Technician not found"));
        if (user.getRole() != Role.TECHNICIAN) throw new NotFoundException("Technician not found");
        return user;
    }

    private RatingResponse map(Rating rating) {
        return new RatingResponse(rating.getId(), rating.getServiceRequest().getId(),
                rating.getRater().getId(), rating.getRater().getFullName(),
                rating.getRatedUser().getId(), rating.getRatedUser().getFullName(),
                rating.getScore(), rating.getComment(),
                rating.getCreatedAt());
    }

    private void recalculate(User user) {
        List<Rating> items = ratings.findByRatedUserIdOrderByCreatedAtDesc(user.getId());
        BigDecimal average = items.isEmpty() ? new BigDecimal("5.00")
                : BigDecimal.valueOf(items.stream().mapToInt(Rating::getScore).average().orElse(5))
                .setScale(2, RoundingMode.HALF_UP);
        user.setAverageRating(average);
        users.save(user);
    }
}
