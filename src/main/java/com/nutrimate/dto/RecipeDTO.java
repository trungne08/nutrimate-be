package com.nutrimate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class RecipeDTO {
    private String id;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String instruction;

    /**
     * Optional: URL ảnh có sẵn (nếu không upload file).
     * Nếu upload file thì field này có thể để trống.
     */
    private String imageUrl;

    @Schema(type = "string", format = "binary", description = "Recipe image file")
    private MultipartFile imageFile;

    private Integer prepTimeMinutes;

    // Dinh dưỡng
    private Integer calories;
    private Float protein;
    private Float carbs;
    private Float fat;

    private Boolean isPremium;
}