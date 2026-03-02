package com.nutrimate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequestDTO {
    @NotNull(message = "Expert ID is required")
    private String expertId;

    @NotNull(message = "Booking time is required")
    private LocalDateTime bookingTime;
    
    private String note;

    // Nếu true, BE sẽ cố gắng dùng lượt free session (nếu còn)
    private Boolean useFreeSession;
}