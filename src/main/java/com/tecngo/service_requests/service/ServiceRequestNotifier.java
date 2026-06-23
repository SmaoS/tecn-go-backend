package com.tecngo.service_requests.service;

import com.tecngo.geolocation.HaversineDistance;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.technicians.entity.TechnicianStatus;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ServiceRequestNotifier {
    private final ApplicationEventPublisher events;
    private final TechnicianProfileRepository technicianProfiles;
    private final HaversineDistance distance;

    @Value("${app.notifications.new-request-radius-km:25}")
    private double newRequestRadiusKm;

    public void nearbyTechnicians(ServiceRequest request) {
        technicianProfiles.findByStatusOrderByCreatedAtAsc(TechnicianStatus.APPROVED).stream()
                .filter(com.tecngo.technicians.entity.TechnicianProfile::isAvailable)
                .filter(profile -> profile.getLatitude() != null && profile.getLongitude() != null)
                .filter(profile -> profile.getCategories().stream()
                        .anyMatch(category -> category.getId().equals(request.getCategory().getId())))
                .filter(profile -> profile.getUser().getCity() == null || request.getCity() == null
                        || profile.getUser().getCity().getId().equals(request.getCity().getId()))
                .filter(profile -> distance.kilometers(profile.getLatitude(), profile.getLongitude(),
                        request.getLatitude(), request.getLongitude()) <= newRequestRadiusKm)
                .forEach(profile -> publish(profile.getUser(),
                        "Nueva solicitud cercana disponible",
                        request.getCategory().getName() + " a menos de "
                                + Math.round(newRequestRadiusKm) + " km",
                        NotificationType.NEW_REQUEST,
                        Map.of("type", "SERVICE_REQUEST",
                                "requestId", request.getId().toString(),
                                "route", "AvailableRequests")));
    }

    public void newQuote(ServiceRequest request, User technician, BigDecimal price) {
        publish(request.getClient(), "Nueva cotización recibida",
                technician.getFullName() + " cotizó " + formatCop(price)
                        + " para tu solicitud en" + request.getCategory().getName(),
                NotificationType.NEW_QUOTE, requestData(request));
    }

    public void quoteAccepted(ServiceRequest request, User technician) {
        publish(technician, "Cotización aceptada por el cliente",
                request.getClient().getFullName() + " aceptó tu cotización para "
                        + request.getCategory().getName(),
                NotificationType.QUOTE_ACCEPTED, requestData(request));
    }

    public void quoteRejected(ServiceRequest request, User technician) {
        publish(technician, "Cotización no seleccionada",
                request.getClient().getFullName() + " eligió otra cotización o rechazó tu oferta para "
                        + request.getCategory().getName(),
                NotificationType.QUOTE_REJECTED,
                Map.of("type", "SERVICE_REQUEST",
                        "requestId", request.getId().toString(),
                        "route", "AvailableRequests"));
    }

    public void statusChanged(ServiceRequest request, RequestStatus status) {
        publish(request.getClient(), statusTitle(status), statusMessage(status),
                notificationType(status), requestData(request));
    }

    public void cancelled(ServiceRequest request, User actor) {
        counterpart(request, actor, "Servicio cancelado", "La solicitud fue cancelada",
                NotificationType.SERVICE_STATUS_CHANGED);
    }

    public void paymentConfirmed(ServiceRequest request) {
        publish(request.getClient(), "Servicio pagado",
                "El técnico confirmó el pago. Puedes calificar el servicio.",
                NotificationType.SERVICE_COMPLETED, requestData(request));
    }

    public void paymentDisputed(ServiceRequest request) {
        publish(request.getClient(), "Pago reportado",
                "El técnico reportó un problema de pago en el servicio.",
                NotificationType.SERVICE_STATUS_CHANGED, requestData(request));
    }

    public void expired(ServiceRequest request) {
        publish(request.getClient(), "Solicitud vencida",
                "La solicitud de " + request.getCategory().getName()
                        + " se canceló automáticamente porque terminó su tiempo de publicación.",
                NotificationType.SERVICE_STATUS_CHANGED, requestData(request));
    }

    private void counterpart(ServiceRequest request, User actor, String title, String message,
                             NotificationType type) {
        User recipient = request.getClient().getId().equals(actor.getId())
                ? request.getTechnician() : request.getClient();
        if (recipient != null) publish(recipient, title, message, type, requestData(request));
    }

    private void publish(User recipient, String title, String message,
                         NotificationType type, Map<String, String> data) {
        events.publishEvent(new UserNotificationEvent(recipient.getId(), title, message, type, data));
    }

    private NotificationType notificationType(RequestStatus status) {
        return switch (status) {
            case ON_THE_WAY -> NotificationType.TECHNICIAN_ON_THE_WAY;
            case ARRIVED -> NotificationType.TECHNICIAN_ARRIVED;
            case IN_PROGRESS -> NotificationType.SERVICE_STARTED;
            case COMPLETED -> NotificationType.SERVICE_COMPLETED;
            default -> NotificationType.SERVICE_STATUS_CHANGED;
        };
    }

    private String statusMessage(RequestStatus status) {
        return switch (status) {
            case ON_THE_WAY -> "El técnico va en camino";
            case ARRIVED -> "El técnico llegó al lugar del servicio";
            case IN_PROGRESS -> "El servicio ha comenzado";
            case COMPLETED -> "El técnico marcó el servicio como completado";
            default -> "Tu servicio cambió a " + status;
        };
    }

    private String statusTitle(RequestStatus status) {
        return switch (status) {
            case ON_THE_WAY -> "Técnico en camino";
            case ARRIVED -> "Técnico llegó al servicio";
            case IN_PROGRESS -> "Servicio iniciado";
            case COMPLETED -> "Servicio finalizado";
            default -> "Estado del servicio actualizado";
        };
    }

    private String formatCop(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO"));
        return "$" + formatter.format(value.setScale(0, java.math.RoundingMode.HALF_UP)) + " COP";
    }

    private Map<String, String> requestData(ServiceRequest request) {
        return Map.of("type", "SERVICE_REQUEST",
                "requestId", request.getId().toString(),
                "route", "RequestDetail");
    }
}
