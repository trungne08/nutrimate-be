package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "`Payments`")
@Data
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "related_id", nullable = false)
    private String relatedId; // ID của Booking hoặc Subscription

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    public enum PaymentType { SUBSCRIPTION, EXPERT_BOOKING }
    public enum PaymentMethod { MOMO, ZALOPAY, BANK, CREDIT }
    public enum PaymentStatus { PENDING, SUCCESS, FAILED }
}