package com.tecngo.service_security.service;

import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.service_requests.entity.ServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ServiceSecurityNotifier {
    private final ApplicationEventPublisher events;

    public void technicianArrived(ServiceRequest request) {
        events.publishEvent(new UserNotificationEvent(request.getClient().getId(),
                "Verifica a tu técnico",
                "El técnico llegó. Verifica su código de seguridad antes de iniciar el servicio.",
                NotificationType.TECHNICIAN_ARRIVED,
                requestData(request)));
    }

    public void securityVerified(ServiceRequest request) {
        if (request.getTechnician() == null) return;
        events.publishEvent(new UserNotificationEvent(request.getTechnician().getId(),
                "Identidad verificada",
                "El cliente verificó tu código. Ya puedes iniciar el servicio.",
                NotificationType.SERVICE_STATUS_CHANGED,
                requestData(request)));
    }

    private Map<String, String> requestData(ServiceRequest request) {
        return Map.of("type", "SERVICE_REQUEST",
                "requestId", request.getId().toString(),
                "route", "RequestDetail");
    }
}
