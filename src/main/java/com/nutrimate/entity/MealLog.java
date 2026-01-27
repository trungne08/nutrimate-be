package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "`Meal_Logs`")
@Data
public class MealLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "meal_log_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "log_id", nullable = false)
    private DailyLog dailyLog;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type")
    private MealType mealType; // Sáng, Trưa, Tối...

    private Double amount; // Số lượng (VD: 1.5 suất)

    @Column(name = "calories_consumed")
    private Integer caloriesConsumed; // Calo thực nạp (= recipe.calories * amount)
}