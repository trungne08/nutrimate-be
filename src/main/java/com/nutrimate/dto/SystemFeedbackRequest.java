package com.nutrimate.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SystemFeedbackRequest {
    
    @NotNull(message = "Vui lòng chọn số sao đánh giá")
    @DecimalMin(value = "1.0", message = "Đánh giá tối thiểu 1.0 sao")
    @DecimalMax(value = "5.0", message = "Đánh giá tối đa 5.0 sao")
    private Double rating; // Đổi sang Double ở đây luôn

    private String content;
}