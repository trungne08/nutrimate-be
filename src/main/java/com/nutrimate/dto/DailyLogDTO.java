package com.nutrimate.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DailyLogDTO {
    private String logId;
    private LocalDate logDate;
    private Integer totalCaloriesIn;
    private String notes;
    
    private List<MealLogDTO> meals; // Danh sách các món đã ăn
}