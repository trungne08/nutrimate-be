package com.nutrimate.dto;

import lombok.Data;

@Data
public class FeedbackCreateRequest {
    private String bookingId;
    private Integer rating;   // Bắt buộc: 1 đến 5
    private String comment;   // Lời nhận xét (Có thể để trống)
}