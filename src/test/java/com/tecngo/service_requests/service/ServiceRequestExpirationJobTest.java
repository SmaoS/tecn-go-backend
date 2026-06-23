package com.tecngo.service_requests.service;

import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ServiceRequestExpirationJobTest {

    @Test
    void cancelsExpiredRequestsAndNotifiesTheClient() {
        ServiceRequestRepository requests = mock(ServiceRequestRepository.class);
        ServiceRequestNotifier notifier = mock(ServiceRequestNotifier.class);
        ServiceRequest expired = ServiceRequest.builder()
                .status(RequestStatus.QUOTE_PENDING)
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(requests.findByStatusAndExpiresAtLessThanEqual(
                eq(RequestStatus.QUOTE_PENDING), any(Instant.class)))
                .thenReturn(List.of(expired));

        new ServiceRequestExpirationJob(requests, notifier).cancelExpiredRequests();

        assertThat(expired.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        verify(notifier).expired(expired);
    }
}
