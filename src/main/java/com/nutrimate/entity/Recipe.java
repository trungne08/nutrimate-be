package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "`Recipes`")
@Data
public class Recipe {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "recipe_id")
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String instruction;
    
    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;

    private Integer calories;
    
    @Column(name = "protein_g")
    private Float protein;
    
    @Column(name = "carbs_g")
    private Float carbs;
    
    @Column(name = "fat_g")
    private Float fat;

    @Column(name = "is_premium")
    private Boolean isPremium;
}