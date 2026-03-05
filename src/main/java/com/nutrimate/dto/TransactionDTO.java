package com.nutrimate.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TransactionDTO {
    private Long orderCode;        // Mã đơn hàng PayOS
    private String userFullName;   // Tên người thanh toán
    private String userEmail;      // Email
    private String type;           // Phân loại: "BOOKING" hoặc "SUBSCRIPTION"
    private String description;    // Chi tiết: "Đặt lịch khám..." hoặc "Mua gói VIP..."
    private Double amount;         // Số tiền
    private String status;         // Trạng thái: PENDING, CONFIRMED, ACTIVE, CANCELLED...
    private LocalDateTime transactionDate; // Ngày giao dịch
}