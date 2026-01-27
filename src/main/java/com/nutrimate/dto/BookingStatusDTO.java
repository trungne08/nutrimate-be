package com.nutrimate.dto;

import lombok.Data;

@Data
public class BookingStatusDTO {
    private String status; // CONFIRMED, CANCELLED, COMPLETED
}