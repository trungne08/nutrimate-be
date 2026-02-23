package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Data
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Đánh giá cho giao dịch/lịch hẹn nào
    @OneToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // Ai là người đánh giá (Khách hàng)
    @ManyToOne
    @JoinColumn(name = "member_id")
    private User member;

    // Chuyên gia bị đánh giá
    @ManyToOne
    @JoinColumn(name = "expert_id")
    private ExpertProfile expert;

    // Số sao (1 đến 5)
    @Column(nullable = false)
    private Integer rating;

    // Lời nhận xét
    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}