package com.nutrimate.dto;

import com.nutrimate.entity.HealthProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Request DTO for creating/updating health profile")
public class HealthProfileRequest {
    
    @NotNull(message = "Gender is required")
    @Schema(description = "Gender", example = "Male", allowableValues = {"Male", "Female", "Other"}, required = true)
    private HealthProfile.Gender gender;
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Schema(description = "Date of birth (YYYY-MM-DD)", example = "1990-01-15", required = true)
    private LocalDate dateOfBirth;
    
    @NotNull(message = "Height is required")
    @Positive(message = "Height must be positive")
    @Min(value = 50, message = "Height must be at least 50 cm")
    @Max(value = 250, message = "Height must not exceed 250 cm")
    @Schema(description = "Height in centimeters", example = "170.0", minimum = "50", maximum = "250", required = true)
    private Float heightCm;
    
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    @Min(value = 20, message = "Weight must be at least 20 kg")
    @Max(value = 300, message = "Weight must not exceed 300 kg")
    @Schema(description = "Current weight in kilograms", example = "70.0", minimum = "20", maximum = "300", required = true)
    private Float weightKg;
    
    @Positive(message = "Target weight must be positive")
    @Min(value = 20, message = "Target weight must be at least 20 kg")
    @Max(value = 300, message = "Target weight must not exceed 300 kg")
    @Schema(description = "Target weight in kilograms (optional)", example = "65.0", minimum = "20", maximum = "300")
    private Float targetWeightKg;
    
    @Schema(description = "Activity level (optional)", example = "MODERATELY_ACTIVE", 
            allowableValues = {"SEDENTARY", "LIGHTLY_ACTIVE", "MODERATELY_ACTIVE", "VERY_ACTIVE"})
    private HealthProfile.ActivityLevel activityLevel;
    
    @Schema(description = "Dietary preference (optional)", example = "CLEAN_EATING", 
            allowableValues = {"NONE", "VEGAN", "KETO", "CLEAN_EATING", "BALANCED"})
    private HealthProfile.DietaryPreference dietaryPreference;
}
