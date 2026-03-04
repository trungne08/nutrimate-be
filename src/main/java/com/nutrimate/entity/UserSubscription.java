package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "`User_Subscriptions`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "subscription_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "order_code", unique = true)
    private Long orderCode;

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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SubscriptionStatus {
        Active, Expired, Cancelled
    }
}