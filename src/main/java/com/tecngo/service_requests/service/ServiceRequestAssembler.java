package com.tecngo.service_requests.service;

import com.tecngo.content_moderation.entity.ModerationStatus;
import com.tecngo.geolocation.LocationPrecision;
import com.tecngo.service_requests.dto.ServiceQuoteResponse;
import com.tecngo.service_requests.dto.ServiceRequestImageResponse;
import com.tecngo.service_requests.dto.ServiceRequestResponse;
import com.tecngo.service_requests.entity.QuoteStatus;
import com.tecngo.service_requests.entity.ServiceQuote;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.entity.ServiceRequestImage;
import com.tecngo.service_requests.repository.ServiceRequestImageRepository;
import com.tecngo.service_requests.repository.ServiceQuoteRepository;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServiceRequestAssembler {
    private final ServiceRequestImageRepository images;
    private final TechnicianProfileRepository technicianProfiles;
    private final ServiceQuoteRepository quotes;

    public ServiceRequestResponse response(ServiceRequest item) {
        return response(item, null, false);
    }

    public ServiceRequestResponse response(ServiceRequest item, Double distanceKm, boolean approximateLocation) {
        Map<UUID, List<ServiceRequestImage>> imageMap = loadImages(List.of(item.getId()));
        Map<UUID, List<String>> categoryMap = loadTechnicianCategories(
                item.getTechnician() == null ? List.of() : List.of(item.getTechnician().getId()));
        return response(item, distanceKm, approximateLocation,
                imageMap.getOrDefault(item.getId(), List.of()),
                item.getTechnician() == null
                        ? List.of()
                        : categoryMap.getOrDefault(item.getTechnician().getId(), List.of()));
    }

    public List<ServiceRequestResponse> responses(List<ServiceRequest> items,
                                                  Map<UUID, Double> distances,
                                                  boolean approximateLocation) {
        return responses(items, distances, approximateLocation, null);
    }

    public List<ServiceRequestResponse> responses(List<ServiceRequest> items,
                                                  Map<UUID, Double> distances,
                                                  boolean approximateLocation,
                                                  User viewer) {
        if (items.isEmpty()) return List.of();
        Map<UUID, List<ServiceRequestImage>> imageMap =
                loadImages(items.stream().map(ServiceRequest::getId).toList());
        Map<UUID, List<String>> categoryMap = loadTechnicianCategories(items.stream()
                .map(ServiceRequest::getTechnician)
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .distinct()
                .toList());
        List<UUID> pendingQuoteRequestIds = loadPendingQuoteRequestIds(items, viewer);
        return items.stream().map(item -> {
            UUID technicianId = item.getTechnician() == null ? null : item.getTechnician().getId();
            return response(item, distances.get(item.getId()), approximateLocation,
                    imageMap.getOrDefault(item.getId(), List.of()),
                    technicianId == null ? List.of() : categoryMap.getOrDefault(technicianId, List.of()),
                    pendingQuoteRequestIds.contains(item.getId()));
        }).toList();
    }

    public Page<ServiceRequestResponse> page(Page<ServiceRequest> page) {
        return new PageImpl<>(responses(page.getContent(), Map.of(), false),
                page.getPageable(), page.getTotalElements());
    }

    public ServiceQuoteResponse quote(ServiceQuote quote) {
        List<String> categories = loadTechnicianCategories(List.of(quote.getTechnician().getId()))
                .getOrDefault(quote.getTechnician().getId(), List.of());
        return quote(quote, categories);
    }

    public List<ServiceQuoteResponse> quotes(List<ServiceQuote> items) {
        Map<UUID, List<String>> categoryMap = loadTechnicianCategories(items.stream()
                .map(item -> item.getTechnician().getId())
                .distinct()
                .toList());
        return items.stream()
                .map(item -> quote(item,
                        categoryMap.getOrDefault(item.getTechnician().getId(), List.of())))
                .toList();
    }

    private ServiceRequestResponse response(
            ServiceRequest item,
            Double distanceKm,
            boolean approximateLocation,
            List<ServiceRequestImage> serviceImages,
            List<String> technicianCategories
    ) {
        return response(item, distanceKm, approximateLocation, serviceImages,
                technicianCategories, false);
    }

    private ServiceRequestResponse response(
            ServiceRequest item,
            Double distanceKm,
            boolean approximateLocation,
            List<ServiceRequestImage> serviceImages,
            List<String> technicianCategories,
            boolean myPendingQuote
    ) {
        User technician = item.getTechnician();
        return new ServiceRequestResponse(item.getId(), item.getClient().getId(), item.getClient().getFullName(),
                technician == null ? null : technician.getId(), technician == null ? null : technician.getFullName(),
                item.getClient().getProfilePhotoUrl(), item.getClient().getAverageRating(),
                item.getClient().getPaidServicesCount(),
                technician == null ? null : technician.getProfilePhotoUrl(),
                technician == null ? null : technician.getAverageRating(),
                technician == null ? 0 : technician.getCompletedServicesCount(),
                technician == null ? null : technician.getWorkExperienceDescription(),
                technicianCategories,
                technician != null && technician.isDocumentsVerified()
                        && !blank(technician.getCertificatePhotoUrl()),
                item.getCategory().getId(), item.getCategory().getName(), item.getDescription(),
                approximateLocation ? approximate(item.getAddress()) : item.getAddress(),
                approximateLocation ? approximateCoordinate(item.getLatitude()) : item.getLatitude(),
                approximateLocation ? approximateCoordinate(item.getLongitude()) : item.getLongitude(),
                approximateLocation ? LocationPrecision.APPROXIMATE : LocationPrecision.EXACT,
                distanceKm, item.getEstimatedPrice(),
                item.getTechnicianPrice(), item.getFinalPrice(), item.getRequestedPaymentMethod(),
                item.getStatus(), item.getCreatedAt(),
                serviceImages.size(),
                serviceImages.isEmpty() ? null : serviceImages.getFirst().getImageUrl(),
                serviceImages.stream().map(image -> new ServiceRequestImageResponse(
                        image.getId(), item.getId(), image.getImageUrl(), image.getPublicId(),
                        image.getContentAsset() == null ? null : image.getContentAsset().getId(),
                        image.getContentAsset() == null ? ModerationStatus.APPROVED
                                : image.getContentAsset().getModerationStatus(),
                        image.getCreatedAt())).toList(),
                item.getCity() == null ? null : item.getCity().getId(),
                item.getCity() == null ? null : item.getCity().getName(),
                myPendingQuote);
    }

    private ServiceQuoteResponse quote(ServiceQuote quote, List<String> technicianCategories) {
        User technician = quote.getTechnician();
        return new ServiceQuoteResponse(
                quote.getId(), quote.getServiceRequest().getId(), technician.getId(),
                technician.getFullName(), technician.getProfilePhotoUrl(),
                technician.getAverageRating(), technician.getCompletedServicesCount(),
                technician.getWorkExperienceDescription(), technicianCategories,
                technician.isDocumentsVerified() && !blank(technician.getCertificatePhotoUrl()),
                quote.getPrice(), quote.getDescription(), quote.getStatus(), quote.getCreatedAt(),
                quote.getUpdatedAt(), quote.getExpiresAt(), quote.getRespondedAt());
    }

    private Map<UUID, List<ServiceRequestImage>> loadImages(List<UUID> requestIds) {
        if (requestIds.isEmpty()) return Map.of();
        return images.findByServiceRequestIdInOrderByCreatedAtAsc(requestIds).stream()
                .filter(image -> image.getContentAsset() == null
                        || image.getContentAsset().getModerationStatus() == ModerationStatus.APPROVED)
                .collect(Collectors.groupingBy(image -> image.getServiceRequest().getId()));
    }

    private Map<UUID, List<String>> loadTechnicianCategories(Collection<UUID> technicianIds) {
        if (technicianIds.isEmpty()) return Map.of();
        return technicianProfiles.findWithCategoriesByUserIdIn(technicianIds).stream()
                .collect(Collectors.toMap(
                        profile -> profile.getUser().getId(),
                        profile -> profile.getCategories().stream()
                                .map(category -> category.getName())
                                .sorted()
                                .toList()));
    }

    private List<UUID> loadPendingQuoteRequestIds(List<ServiceRequest> requests, User viewer) {
        if (viewer == null || requests.isEmpty()) return List.of();
        List<UUID> requestIds = requests.stream().map(ServiceRequest::getId).toList();
        return quotes.findByServiceRequestIdInAndTechnicianIdAndStatus(
                        requestIds, viewer.getId(), QuoteStatus.PENDING).stream()
                .map(quote -> quote.getServiceRequest().getId())
                .distinct()
                .toList();
    }

    private String approximate(String address) {
        String[] parts = address.split(",");
        if (parts.length < 2) return "Zona cercana";
        int start = Math.max(1, parts.length - 2);
        return String.join(", ", java.util.Arrays.stream(parts, start, parts.length)
                .map(String::trim).toList());
    }

    private Double approximateCoordinate(Double coordinate) {
        if (coordinate == null) return null;
        return Math.round(coordinate * 100.0) / 100.0;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
