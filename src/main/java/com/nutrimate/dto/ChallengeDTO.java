package com.nutrimate.dto;

import com.nutrimate.entity.Challenge;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChallengeDTO {
    // Request tạo mới
    @Data
    public static class CreateRequest {
        @NotNull private String title;
        private String description;
        @NotNull private Integer durationDays;
        private Challenge.ChallengeLevel level;
    }

    // Response cho User (Kèm tiến độ nếu đã tham gia)
    @Data
    public static class Response {
        private String id;
        private String title;
        private String description;
        private Integer durationDays;
        private String level;
        
        // Thông tin riêng của User (nếu có)
        private boolean isJoined;
        private Integer daysCompleted;
        private String status;
    }
}