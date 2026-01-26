package com.nutrimate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDTO {
    @NotNull(message = "Plan ID is required")
    private String planId;

    @NotNull(message = "Payment method is required")
    private String paymentMethod; // MOMO, ZALOPAY...
}