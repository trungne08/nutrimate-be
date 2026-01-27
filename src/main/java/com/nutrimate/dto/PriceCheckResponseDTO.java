package com.nutrimate.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PriceCheckResponseDTO {
    private boolean isFreeSession;
    private BigDecimal originalPrice;
    private BigDecimal finalPrice;
    private String message; // VD: "Bạn còn 2 lượt miễn phí" hoặc "Bạn đã hết lượt"
}