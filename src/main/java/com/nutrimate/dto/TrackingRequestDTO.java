package com.nutrimate.dto;

import com.nutrimate.entity.MealType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TrackingRequestDTO {
    // Dùng cho API tạo log (7.2)
    @Data
    public static class AddFoodLog {
        @NotNull
        private LocalDate date; // Ngày ăn
        @NotNull
        private String recipeId;
        @NotNull
        private MealType mealType;
        @NotNull
        private Double amount; // VD: 1.0 là 1 suất
    }

    // Dùng cho API sửa log (7.3)
    @Data
    public static class UpdateFoodLog {
        @NotNull
        private Double amount;
    }
}