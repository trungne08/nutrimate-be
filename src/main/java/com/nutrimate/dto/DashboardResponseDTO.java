package com.nutrimate.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DashboardResponseDTO {
    private BigDecimal totalRevenue;
    private long totalUsers;
    private long totalTransactions;
    private long totalFeedbacks;
}