package com.nutrimate.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExpertProfileDTO {
    private String expertId;
    private String userId;
    private String fullName; // Lấy từ User entity
    private String avatarUrl; // Lấy từ User entity
    
    private String specialization;
    private String certification;
    private String bio;
    private Integer yearsExperience;
    private Float rating;
    private BigDecimal hourlyRate;
}