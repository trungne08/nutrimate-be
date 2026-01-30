package com.nutrimate.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecipeDTO {
    private String id;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    private String instruction;
    private MultipartFile imageUrl;
    private Integer prepTimeMinutes;
    
    // Dinh dưỡng
    private Integer calories;
    private Float protein;
    private Float carbs;
    private Float fat;
    
    private Boolean isPremium;
}