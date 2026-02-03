package com.nutrimate.dto;

import com.nutrimate.entity.Booking.BookingStatus;
import lombok.Data;

@Data
public class BookingStatusDTO {
    private BookingStatus status;
    private String note;
}