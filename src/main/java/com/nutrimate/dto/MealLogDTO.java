package com.nutrimate.dto;

import lombok.Data;

@Data
public class MealLogDTO {
    private String id;
    
    // Nếu chọn từ Recipe có sẵn
    private String recipeId;
    private String recipeTitle;
    
    // Nếu nhập tay
    private String mealName;
    
    private Integer calories;
}