package com.tecngo.service_requests.service;

import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestExpirationJob {
    private final ServiceRequestRepository requests;
    private final ServiceRequestNotifier notifier;

    @Scheduled(fixedDelayString = "${app.parameters.service-request-expiration-polling-ms:60000}")
    @Transactional
    public void cancelExpiredRequests() {
        var expired = requests.findByStatusAndExpiresAtLessThanEqual(
                RequestStatus.QUOTE_PENDING, Instant.now());
        expired.forEach(request -> {
            request.setStatus(RequestStatus.CANCELLED);
            notifier.expired(request);
        });
        if (!expired.isEmpty()) {
            log.info("Automatically cancelled {} expired service requests", expired.size());
        }
    }
}
