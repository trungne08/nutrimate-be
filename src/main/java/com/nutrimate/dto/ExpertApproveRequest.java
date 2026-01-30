package com.nutrimate.dto;

import lombok.Data;

@Data
public class ExpertApproveRequest {
    // Swagger sẽ hiện rõ trường này để bạn nhập
    // Giá trị hợp lệ: "APPROVED" hoặc "REJECTED"
    private String status; 
}