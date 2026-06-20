package com.tecngo.ratings.service;

import com.tecngo.ratings.dto.CreateRatingRequest;
import com.tecngo.ratings.repository.RatingRepository;
import com.tecngo.referrals.service.ReferralService;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingSelfActionGuardTest {
    @Mock
    private RatingRepository ratings;
    @Mock
    private ServiceRequestRepository requests;
    @Mock
    private UserRepository users;
    @Mock
    private ApplicationEventPublisher events;
    @Mock
    private ReferralService referrals;
    @InjectMocks
    private RatingService service;

    @Test
    void sameAccountCannotRateItselfThroughBothRoles() {
        UUID requestId = UUID.randomUUID();
        User user = User.builder()
                .id(UUID.randomUUID())
                .role(Role.CLIENT)
                .roles(new LinkedHashSet<>(List.of(Role.CLIENT, Role.TECHNICIAN)))
                .activeMode(ActiveMode.CLIENT)
                .build();
        ServiceRequest request = ServiceRequest.builder()
                .id(requestId)
                .client(user)
                .technician(user)
                .status(RequestStatus.PAID)
                .build();
        when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.create(requestId, new CreateRatingRequest(5, "Bien"), user))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("A user cannot rate their own account");
    }
}
