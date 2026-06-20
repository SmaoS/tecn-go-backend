package com.tecngo.technician_location.service;

import com.tecngo.geolocation.HaversineDistance;
import com.tecngo.geolocation.LocationPrecision;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.technician_location.entity.TechnicianLocation;
import com.tecngo.technician_location.repository.TechnicianLocationRepository;
import com.tecngo.technicians.service.TechnicianProfileService;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.technicians.entity.TechnicianProfile;
import com.tecngo.technicians.entity.TechnicianStatus;
import com.tecngo.users.entity.User;
import com.tecngo.shared.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianLocationServiceTest {
    @Mock
    TechnicianLocationRepository locations;
    @Mock
    TechnicianProfileService profiles;
    @Mock
    ServiceRequestRepository requests;
    @Mock
    SystemParameterService parameters;
    @Mock
    TechnicianProfileRepository profileRepository;
    @Mock
    HaversineDistance distance;
    @InjectMocks
    TechnicianLocationService service;

    @Test
    void staleLocationIsReportedOffline() {
        User technician = User.builder().id(UUID.randomUUID()).fullName("Técnico").build();
        TechnicianLocation location = TechnicianLocation.builder()
                .technician(technician)
                .latitude(4.7)
                .longitude(-74.0)
                .online(true)
                .updatedAt(Instant.now().minus(4, ChronoUnit.MINUTES))
                .build();
        when(parameters.technicianOfflineAfterMinutes()).thenReturn(3);
        when(locations.findAll()).thenReturn(List.of(location));

        assertThat(service.all().getFirst().online()).isFalse();
        assertThat(service.all().getFirst().locationPrecision()).isEqualTo(LocationPrecision.EXACT);
    }

    @Test
    void nearbyTechniciansExposeOnlyApproximateCoordinates() {
        User technician = User.builder().id(UUID.randomUUID()).fullName("Técnico").build();
        TechnicianLocation location = TechnicianLocation.builder()
                .technician(technician)
                .latitude(4.12345)
                .longitude(-73.67891)
                .online(true)
                .updatedAt(Instant.now())
                .build();
        TechnicianProfile profile = TechnicianProfile.builder()
                .user(technician)
                .status(TechnicianStatus.APPROVED)
                .available(true)
                .build();
        when(parameters.technicianOfflineAfterMinutes()).thenReturn(3);
        when(locations.findAll()).thenReturn(List.of(location));
        when(profileRepository.findByUserId(technician.getId())).thenReturn(java.util.Optional.of(profile));
        when(distance.kilometers(4.1, -73.6, 4.12345, -73.67891)).thenReturn(5.0);

        var result = service.nearby(4.1, -73.6, 25, null).getFirst();

        assertThat(result.latitude()).isEqualTo(4.12);
        assertThat(result.longitude()).isEqualTo(-73.68);
        assertThat(result.locationPrecision()).isEqualTo(LocationPrecision.APPROXIMATE);
    }

    @Test
    void exactTechnicianLocationRequiresAcceptedQuote() {
        User client = User.builder().id(UUID.randomUUID()).build();
        User technician = User.builder().id(UUID.randomUUID()).fullName("Técnico").build();
        UUID requestId = UUID.randomUUID();
        ServiceRequest pending = ServiceRequest.builder()
                .id(requestId).client(client).technician(technician)
                .status(RequestStatus.QUOTE_PENDING).build();
        when(requests.findById(requestId)).thenReturn(java.util.Optional.of(pending));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.forRequest(requestId, client))
                .isInstanceOf(ForbiddenException.class);

        pending.setStatus(RequestStatus.QUOTE_ACCEPTED);
        TechnicianLocation location = TechnicianLocation.builder()
                .technician(technician).latitude(4.12345).longitude(-73.67891)
                .online(true).updatedAt(Instant.now()).build();
        when(locations.findByTechnicianId(technician.getId()))
                .thenReturn(java.util.Optional.of(location));
        when(parameters.technicianOfflineAfterMinutes()).thenReturn(3);

        var result = service.forRequest(requestId, client);

        assertThat(result.latitude()).isEqualTo(4.12345);
        assertThat(result.longitude()).isEqualTo(-73.67891);
        assertThat(result.locationPrecision()).isEqualTo(LocationPrecision.EXACT);
    }
}
