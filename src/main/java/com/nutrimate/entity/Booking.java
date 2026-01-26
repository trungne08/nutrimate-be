package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Bookings")
@Data
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private User member;

    @ManyToOne
    @JoinColumn(name = "expert_user_id")
    private User expert;

    @Column(name = "booking_time")
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