package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Meal_Logs")
@Data
public class MealLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "meal_log_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "log_id")
    private DailyLog dailyLog;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(name = "meal_name")
    private String mealName; // Dùng khi nhập tay món ngoài

    private Integer calories;
}