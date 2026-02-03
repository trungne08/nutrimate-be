package com.nutrimate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecipeDTO {
    private String id;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    private String instruction;
    /**
     * Optional: URL ảnh có sẵn (nếu không upload file).
     * Upload file thì dùng field multipart tên "imageFile" ở API create/update.
     */
    private String imageUrl;
    private Integer prepTimeMinutes;
    
    // Dinh dưỡng
    private Integer calories;
    private Float protein;
    private Float carbs;
    private Float fat;
    
    private Boolean isPremium;
}