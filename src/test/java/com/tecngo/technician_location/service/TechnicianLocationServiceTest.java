package com.tecngo.technician_location.service;

import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.technician_location.entity.TechnicianLocation;
import com.tecngo.technician_location.repository.TechnicianLocationRepository;
import com.tecngo.technicians.service.TechnicianProfileService;
import com.tecngo.users.entity.User;
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
    }
}
