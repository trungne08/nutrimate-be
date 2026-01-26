package com.nutrimate.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ChallengeDTO {
    private String id;
    private String title;
    private String description;
    private Integer durationDays;
    
    // Dành cho response khi user đang tham gia
    private String status; // In Progress / Completed
    private LocalDate joinDate;
    private Integer daysCompleted; // Tính toán từ joinDate
}