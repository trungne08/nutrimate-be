package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
    @JoinColumn(name = "member_id", nullable = false) // DB gần như chắc chắn NOT NULL: một booking luôn phải có member
    private User member;

    @ManyToOne
    @JoinColumn(name = "expert_user_id", nullable = false) // DB đang NOT NULL nên đánh dấu non-null để pass validate
    private User expert;

    @Column(name = "booking_time", nullable = false) // DB đang NOT NULL nên model phải non-null để pass validate
    private LocalDateTime bookingTime;

    // --- Logic giá ---
    @Column(name = "is_free_session")
    private Boolean isFreeSession;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "final_price")
    private BigDecimal finalPrice;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "meeting_link")
    private String meetingLink;

    public enum BookingStatus {
        Pending, Confirmed, Completed, Cancelled
    }
}