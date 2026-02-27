package com.nutrimate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentLinkRequestDTO {

    @NotBlank(message = "userId là bắt buộc")
    private String userId;

    @NotBlank(message = "planId là bắt buộc")
    private String planId;

    @NotNull(message = "amount là bắt buộc")
    @Min(value = 1, message = "amount phải lớn hơn 0")
    private Integer amount;
}
