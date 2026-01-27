package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "`User_Subscriptions`")
@Data
public class UserSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "subscription_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status; // Active, Expired, Cancelled

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew;

    public enum SubscriptionStatus {
        Active, Expired, Cancelled
    }
}