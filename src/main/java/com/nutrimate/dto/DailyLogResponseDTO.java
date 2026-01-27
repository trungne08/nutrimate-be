package com.nutrimate.dto;

import com.nutrimate.entity.MealType;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DailyLogResponseDTO {
    private String logId;
    private LocalDate date;
    private Integer totalCalories;
    private List<MealLogDTO> meals;

    @Data
    public static class MealLogDTO {
        private String mealLogId;
        private String recipeName;
        private String recipeId;
        private MealType mealType;
        private Double amount;
        private Integer calories;
        private String image; // Nếu có
    }
}