package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "`User_Benefit_Usage`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBenefitUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "usage_id")
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = false)
    private UserSubscription subscription;

    // --- Cũ: Dành cho Expert ---
    @Column(name = "sessions_used")
    private Integer sessionsUsed; 

    // --- MỚI: Dành cho Recipe Limit ---
    @Column(name = "daily_recipe_views", nullable = false)
    private Integer dailyRecipeViews = 0; // Đếm số bài đã xem hôm nay

    @Column(name = "last_recipe_view_date", nullable = false)
    private LocalDate lastRecipeViewDate; // Ngày xem gần nhất để reset
}