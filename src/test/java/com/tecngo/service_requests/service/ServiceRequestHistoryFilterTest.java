package com.tecngo.service_requests.service;

import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRequestHistoryFilterTest {
    @Mock ServiceRequestRepository requests;
    @Mock com.tecngo.service_requests.repository.ServiceQuoteRepository quotes;
    @Mock com.tecngo.service_requests.repository.ServiceRequestImageRepository images;
    @Mock com.tecngo.services.service.ServiceCategoryService categories;
    @Mock com.tecngo.technicians.service.TechnicianProfileService technicianProfiles;
    @Mock com.tecngo.technicians.repository.TechnicianProfileRepository technicianProfileRepository;
    @Mock com.tecngo.geolocation.HaversineDistance distance;
    @Mock org.springframework.context.ApplicationEventPublisher events;
    @Mock com.tecngo.verification.service.EmailVerificationService emailVerification;
    @Mock com.tecngo.system_parameters.service.SystemParameterService parameters;
    @Mock com.tecngo.technician_location.repository.TechnicianLocationRepository technicianLocations;
    @Mock com.tecngo.users.service.UserAccessService userAccess;
    @Mock com.tecngo.legal.service.LegalService legal;
    @InjectMocks ServiceRequestService service;

    @Test
    void clientActiveRequestsExcludePaidAndCancelled() {
        User client = User.builder().id(UUID.randomUUID()).role(Role.CLIENT).build();
        when(requests.findByClientIdAndStatusInOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(client.getId()), org.mockito.ArgumentMatchers.anySet()))
                .thenReturn(List.of());

        service.clientRequests(client, true);

        ArgumentCaptor<Set<RequestStatus>> statuses = ArgumentCaptor.forClass(Set.class);
        verify(requests).findByClientIdAndStatusInOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(client.getId()), statuses.capture());
        assertThat(statuses.getValue()).doesNotContain(RequestStatus.PAID, RequestStatus.CANCELLED)
                .contains(RequestStatus.COMPLETED, RequestStatus.QUOTE_PENDING);
    }

    @Test
    void assignedHistoryUsesClosedStatuses() {
        User technician = User.builder().id(UUID.randomUUID()).role(Role.TECHNICIAN).build();
        when(requests.findByTechnicianIdAndStatusInOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(technician.getId()), org.mockito.ArgumentMatchers.anySet()))
                .thenReturn(List.of());

        service.assignedHistory(technician);

        ArgumentCaptor<Set<RequestStatus>> statuses = ArgumentCaptor.forClass(Set.class);
        verify(requests).findByTechnicianIdAndStatusInOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(technician.getId()), statuses.capture());
        assertThat(statuses.getValue()).containsExactlyInAnyOrder(RequestStatus.PAID, RequestStatus.CANCELLED);
    }
}
