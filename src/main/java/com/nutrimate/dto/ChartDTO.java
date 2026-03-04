package com.nutrimate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartDTO {
    private String label;  // Ví dụ: "2026-02", "2026", "Premium Plan"
    private Double value;  // Số lượng User, hoặc Tổng tiền, hoặc Số %
}