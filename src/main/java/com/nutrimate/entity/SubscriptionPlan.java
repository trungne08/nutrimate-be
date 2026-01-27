package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "`Subscription_Plans`")
@Data
public class SubscriptionPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "plan_id")
    private String id;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    // --- Feature Flags (Quyền lợi) ---
    @Column(name = "can_access_basic_content", nullable = false)
    private Boolean canAccessBasicContent;

    @Column(name = "can_use_ai_coach", nullable = false)
    private Boolean canUseAiCoach;

    @Column(name = "is_expert_plan", nullable = false)
    private Boolean isExpertPlan;

    @Column(name = "free_sessions_per_cycle", nullable = false)
    private Integer freeSessionsPerCycle;

    @Column(name = "description", nullable = false)
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}