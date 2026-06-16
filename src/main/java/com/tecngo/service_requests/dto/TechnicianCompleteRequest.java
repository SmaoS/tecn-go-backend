package com.tecngo.service_requests.dto;

import com.tecngo.payments.entity.PaymentMethod;
import jakarta.validation.constraints.Size;

public record TechnicianCompleteRequest(
        boolean paymentReceived,
        PaymentMethod paymentMethod,
        @Size(max = 2000) String comment
) {}
