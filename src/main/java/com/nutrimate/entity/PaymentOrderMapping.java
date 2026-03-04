package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lưu mapping orderCode (PayOS) -> userId, planId để khi webhook trả về có thể tạo UserSubscription.
 * Dùng cho thanh toán Subscription, không dùng cho Booking.
 */
@Entity
@Table(name = "`Payment_Order_Mappings`", indexes = @Index(name = "idx_order_code", columnList = "order_code", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "plan_id", nullable = false)
    private String planId;

    @Column(name = "amount")
    private Long amount; // Số tiền (VNĐ) đã thanh toán - lưu để tạo Payment record

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
