package com.nutrimate.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingResponseDTO {
    private String bookingId;
    
    // Thông tin người đặt hoặc chuyên gia (tùy ngữ cảnh xem)
    private String memberId;
    private String memberName;
    private String expertId;
    private String expertName;
    
    private LocalDateTime bookingTime;
    private String status; // Pending, Confirmed...
    private String meetingLink;
    
    // Thông tin giá
    private Boolean isFreeSession;
    private BigDecimal finalPrice;
}