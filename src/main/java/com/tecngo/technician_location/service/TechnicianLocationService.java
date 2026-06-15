package com.tecngo.technician_location.service;

import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.technician_location.dto.TechnicianLocationRequest;
import com.tecngo.technician_location.dto.TechnicianLocationResponse;
import com.tecngo.technician_location.dto.NearbyTechnicianResponse;
import com.tecngo.technician_location.entity.TechnicianLocation;
import com.tecngo.technician_location.repository.TechnicianLocationRepository;
import com.tecngo.technicians.service.TechnicianProfileService;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.tecngo.geolocation.HaversineDistance;
import com.tecngo.technicians.entity.TechnicianStatus;

@Service
@RequiredArgsConstructor
public class TechnicianLocationService {
    private static final Set<RequestStatus> TRACKABLE = Set.of(
            RequestStatus.QUOTE_ACCEPTED, RequestStatus.ON_THE_WAY, RequestStatus.ARRIVED,
            RequestStatus.IN_PROGRESS);

    private final TechnicianLocationRepository locations;
    private final TechnicianProfileService profiles;
    private final TechnicianProfileRepository profileRepository;
    private final ServiceRequestRepository requests;
    private final SystemParameterService parameters;
    private final HaversineDistance distance;

    @Transactional
    public TechnicianLocationResponse update(User technician, TechnicianLocationRequest request) {
        profiles.approvedProfile(technician);
        TechnicianLocation location = locations.findByTechnicianId(technician.getId())
                .orElseGet(() -> TechnicianLocation.builder().technician(technician).build());
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setAccuracy(request.accuracy());
        location.setSpeed(request.speed());
        location.setHeading(request.heading());
        location.setOnline(request.online());
        return map(locations.save(location));
    }

    @Transactional(readOnly = true)
    public TechnicianLocationResponse mine(User technician) {
        profiles.approvedProfile(technician);
        return map(require(technician.getId()));
    }

    @Transactional(readOnly = true)
    public List<TechnicianLocationResponse> all() {
        return locations.findAll().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<NearbyTechnicianResponse> nearby(double latitude, double longitude, double radiusKm) {
        if (radiusKm <= 0 || radiusKm > 100) {
            throw new IllegalArgumentException("radiusKm must be greater than 0 and at most 100");
        }
        Instant threshold = Instant.now().minus(parameters.technicianOfflineAfterMinutes(), ChronoUnit.MINUTES);
        return locations.findAll().stream()
                .filter(TechnicianLocation::isOnline)
                .filter(item -> !item.getUpdatedAt().isBefore(threshold))
                .filter(item -> profileRepository.findByUserId(item.getTechnician().getId())
                        .map(profile -> profile.getStatus() == TechnicianStatus.APPROVED
                                && profile.isAvailable())
                        .orElse(false))
                .map(item -> new NearbyTechnicianResponse(
                        item.getTechnician().getId(),
                        item.getTechnician().getFullName(),
                        item.getTechnician().getProfilePhotoUrl(),
                        item.getTechnician().getAverageRating(),
                        item.getTechnician().getCompletedServicesCount(),
                        item.getLatitude(),
                        item.getLongitude(),
                        distance.kilometers(latitude, longitude, item.getLatitude(), item.getLongitude()),
                        item.getUpdatedAt()))
                .filter(item -> item.distanceKm() <= radiusKm)
                .sorted(java.util.Comparator.comparing(NearbyTechnicianResponse::distanceKm))
                .toList();
    }

    @Transactional(readOnly = true)
    public TechnicianLocationResponse forRequest(UUID requestId, User client) {
        var request = requests.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        if (!request.getClient().getId().equals(client.getId())) {
            throw new ForbiddenException("Only the client owner can view technician location");
        }
        if (request.getTechnician() == null || !TRACKABLE.contains(request.getStatus())) {
            throw new ForbiddenException("Technician location is only available during an active assigned service");
        }
        return map(require(request.getTechnician().getId()));
    }

    private TechnicianLocation require(UUID technicianId) {
        return locations.findByTechnicianId(technicianId)
                .orElseThrow(() -> new NotFoundException("Technician location not found"));
    }

    private TechnicianLocationResponse map(TechnicianLocation item) {
        Instant threshold = Instant.now().minus(
                parameters.technicianOfflineAfterMinutes(), ChronoUnit.MINUTES);
        boolean online = item.isOnline() && !item.getUpdatedAt().isBefore(threshold);
        return new TechnicianLocationResponse(item.getTechnician().getId(),
                item.getTechnician().getFullName(), item.getLatitude(), item.getLongitude(),
                item.getAccuracy(), item.getSpeed(), item.getHeading(), online, item.getUpdatedAt());
    }
}
