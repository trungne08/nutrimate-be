package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "Subscription_Plans")
@Data
public class SubscriptionPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "plan_id")
    private String id;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    private BigDecimal price;

    @Column(name = "duration_days")
    private Integer durationDays;

    // --- Feature Flags (Quyền lợi) ---
    @Column(name = "can_access_basic_content")
    private Boolean canAccessBasicContent;

    @Column(name = "can_use_ai_coach")
    private Boolean canUseAiCoach;

    @Column(name = "is_expert_plan")
    private Boolean isExpertPlan;

    @Column(name = "free_sessions_per_cycle")
    private Integer freeSessionsPerCycle;

    private String description;
    
    @Column(name = "is_active")
    private Boolean isActive;
}