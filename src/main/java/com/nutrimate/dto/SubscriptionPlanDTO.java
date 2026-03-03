package com.nutrimate.dto;

import lombok.Data;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class SubscriptionPlanDTO {
    private String id;
    private String planName;
    private BigDecimal price;
    private Integer durationDays;
    private Boolean canAccessBasicContent;
    private Boolean canUseAiCoach;
    private Boolean isExpertPlan;
    private Integer freeSessionsPerCycle;
    private String description;
    @JsonProperty("isActive")
    private Boolean isActive;
}