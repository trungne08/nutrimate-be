package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "`Bookings`") // Dùng backtick để giữ nguyên chữ hoa trên MySQL (trùng với bảng Bookings hiện tại)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false) // Tên cột trong Database
    @NotFound(action = NotFoundAction.IGNORE)
    private User member;

    @ManyToOne 
    @JoinColumn(name = "expert_user_id", nullable = false) // Tên cột trong Database
    private ExpertProfile expert;

    @Column(name = "booking_time", nullable = false) // DB đang NOT NULL nên model phải non-null để pass validate
    private LocalDateTime bookingTime;

    // --- Logic giá ---
    @Column(name = "is_free_session")
    private Boolean isFreeSession;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "final_price")
    private BigDecimal finalPrice;

    @Column(columnDefinition = "TEXT")
    private String note;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "meeting_link")
    private String meetingLink;

    @Column(name = "order_code")
    private Long orderCode;

    @Column(name = "is_reminded")
    private Boolean isReminded = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum BookingStatus {
        PENDING,
        CONFIRMED,
        REJECTED,
        COMPLETED,
        DONE,
        CANCELLED
    }
}