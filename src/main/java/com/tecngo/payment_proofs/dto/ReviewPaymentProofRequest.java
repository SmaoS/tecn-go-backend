package com.tecngo.payment_proofs.dto;
import jakarta.validation.constraints.Size;
public record ReviewPaymentProofRequest(@Size(max = 1000) String comment) {}
