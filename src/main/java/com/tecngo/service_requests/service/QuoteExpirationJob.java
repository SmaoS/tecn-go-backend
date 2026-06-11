package com.tecngo.service_requests.service;

import com.tecngo.service_requests.repository.ServiceQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class QuoteExpirationJob {
    private final ServiceQuoteRepository quotes;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireQuotes() {
        quotes.expirePending(Instant.now());
    }
}
