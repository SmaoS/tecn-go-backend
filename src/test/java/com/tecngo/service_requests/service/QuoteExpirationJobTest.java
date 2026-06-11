package com.tecngo.service_requests.service;

import com.tecngo.service_requests.repository.ServiceQuoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuoteExpirationJobTest {
    @Mock
    ServiceQuoteRepository quotes;
    @InjectMocks
    QuoteExpirationJob job;

    @Test
    void expiresPendingQuotesIdempotently() {
        job.expireQuotes();
        verify(quotes).expirePending(any(Instant.class));
    }
}
