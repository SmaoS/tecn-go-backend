package com.tecngo.service_requests.service;

import com.tecngo.catalogs.entity.City;
import com.tecngo.geolocation.LocationPrecision;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.services.entity.ServiceCategory;
import com.tecngo.technicians.entity.TechnicianProfile;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ServiceRequestCityFilterTest {
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
    @Mock com.tecngo.catalogs.service.GeographicCatalogService geographicCatalogs;
    @InjectMocks ServiceRequestService service;

    @Test
    void availableRequestsUseTechnicianCityAndExposeOnlyApproximateLocation() {
        City city = City.builder().id(UUID.randomUUID()).name("Villavicencio").build();
        ServiceCategory category = ServiceCategory.builder().id(UUID.randomUUID()).name("Electricista").build();
        User technician = User.builder().id(UUID.randomUUID()).role(Role.TECHNICIAN).city(city).build();
        TechnicianProfile profile = TechnicianProfile.builder().user(technician).latitude(4.1).longitude(-73.6)
                .categories(Set.of(category)).available(false).build();
        User client = User.builder().id(UUID.randomUUID()).fullName("Cliente")
                .averageRating(new BigDecimal("5.00")).build();
        ServiceRequest farther = request(category, city, client, 4.1247, -73.6249);
        ServiceRequest nearby = request(category, city, client, 4.1137, -73.6138);
        when(technicianProfiles.approvedProfile(technician)).thenReturn(profile);
        when(technicianLocations.findByTechnicianId(technician.getId())).thenReturn(java.util.Optional.empty());
        when(requests.findAvailable(org.mockito.ArgumentMatchers.eq(RequestStatus.QUOTE_PENDING),
                org.mockito.ArgumentMatchers.eq(city.getId()),
                org.mockito.ArgumentMatchers.eq(List.of(category.getId())),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(List.of(farther, nearby));
        when(distance.kilometers(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(8.0, 2.0);
        when(images.findByServiceRequestIdInOrderByCreatedAtAsc(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of());

        var result = service.available(technician, null, null, false, null);

        assertThat(result).extracting("id").containsExactly(nearby.getId(), farther.getId());
        assertThat(result).extracting("cityName").containsOnly("Villavicencio");
        assertThat(result).extracting("address").containsOnly("ciudad");
        assertThat(result).extracting("locationPrecision")
                .containsOnly(LocationPrecision.APPROXIMATE);
        assertThat(result.getFirst().latitude()).isEqualTo(4.11);
        assertThat(result.getFirst().longitude()).isEqualTo(-73.61);
        assertThat(result.getLast().latitude()).isEqualTo(4.12);
        assertThat(result.getLast().longitude()).isEqualTo(-73.62);
        verify(images, times(1)).findByServiceRequestIdInOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void optionalRadiusFiltersRequestsWhenExplicitlyEnabled() {
        City city = City.builder().id(UUID.randomUUID()).name("Villavicencio").build();
        ServiceCategory category = ServiceCategory.builder().id(UUID.randomUUID()).name("Electricista").build();
        User technician = User.builder().id(UUID.randomUUID()).role(Role.TECHNICIAN).city(city).build();
        TechnicianProfile profile = TechnicianProfile.builder().user(technician).latitude(4.1).longitude(-73.6)
                .categories(Set.of(category)).build();
        User client = User.builder().id(UUID.randomUUID()).fullName("Cliente")
                .averageRating(new BigDecimal("5.00")).build();
        ServiceRequest farther = request(category, city, client, 4.2, -73.7);
        ServiceRequest nearby = request(category, city, client, 4.11, -73.61);
        when(technicianProfiles.approvedProfile(technician)).thenReturn(profile);
        when(geographicCatalogs.requireCity(city.getId())).thenReturn(city);
        when(technicianLocations.findByTechnicianId(technician.getId())).thenReturn(java.util.Optional.empty());
        when(parameters.serviceSearchMaxRadiusKm()).thenReturn(BigDecimal.valueOf(50));
        when(requests.findAvailable(org.mockito.ArgumentMatchers.eq(RequestStatus.QUOTE_PENDING),
                org.mockito.ArgumentMatchers.eq(city.getId()),
                org.mockito.ArgumentMatchers.eq(List.of(category.getId())),
                org.mockito.ArgumentMatchers.eq(category.getId()),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(List.of(farther, nearby));
        when(distance.kilometers(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(20.0, 2.0);
        when(images.findByServiceRequestIdInOrderByCreatedAtAsc(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of());

        var result = service.available(technician, city.getId(), category.getId(), true, 10.0);

        assertThat(result).extracting("id").containsExactly(nearby.getId());
    }

    @Test
    void legacyTechnicianWithoutCityCanStillSearchByCategoryAndRadius() {
        ServiceCategory category = ServiceCategory.builder().id(UUID.randomUUID()).name("Electricista").build();
        User technician = User.builder().id(UUID.randomUUID()).role(Role.TECHNICIAN).build();
        TechnicianProfile profile = TechnicianProfile.builder().user(technician).latitude(4.1).longitude(-73.6)
                .categories(Set.of(category)).build();
        User client = User.builder().id(UUID.randomUUID()).fullName("Cliente")
                .averageRating(new BigDecimal("5.00")).build();
        ServiceRequest request = request(category, null, client, 4.11, -73.61);
        when(technicianProfiles.approvedProfile(technician)).thenReturn(profile);
        when(technicianLocations.findByTechnicianId(technician.getId())).thenReturn(java.util.Optional.empty());
        when(parameters.serviceSearchMaxRadiusKm()).thenReturn(BigDecimal.valueOf(50));
        when(requests.findAvailableWithoutCity(
                org.mockito.ArgumentMatchers.eq(RequestStatus.QUOTE_PENDING),
                org.mockito.ArgumentMatchers.eq(List.of(category.getId())),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(List.of(request));
        when(distance.kilometers(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(2.0);
        when(images.findByServiceRequestIdInOrderByCreatedAtAsc(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of());

        var result = service.available(technician, null, null, null, 10.0);

        assertThat(result).extracting("id").containsExactly(request.getId());
    }

    private ServiceRequest request(ServiceCategory category, City city, User client,
                                   double latitude, double longitude) {
        return ServiceRequest.builder().id(UUID.randomUUID()).client(client).category(category).city(city)
                .description("Trabajo").address("Zona, ciudad").latitude(latitude).longitude(longitude)
                .status(RequestStatus.QUOTE_PENDING).createdAt(Instant.now()).build();
    }
}
