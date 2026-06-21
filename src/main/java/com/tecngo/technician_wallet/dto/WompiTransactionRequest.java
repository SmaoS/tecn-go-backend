package com.tecngo.technician_wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WompiTransactionRequest(
        @NotBlank @Size(max = 120) String transactionId
) {
}
