package com.nutrimate.dto;

import com.nutrimate.entity.Challenge;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ChallengeDTO {
    // Request tạo mới / cập nhật (multipart/form-data để upload hình)
    @Data
    public static class CreateRequest {
        @NotNull
        private String title;
        private String description;
        @NotNull
        private Integer durationDays;
        private Challenge.ChallengeLevel level;
        private MultipartFile imageFile;  // Upload file hình (ưu tiên)
        private String imageUrl;          // Hoặc truyền URL sẵn (nếu không có file)
    }

    // Response cho User (Kèm tiến độ nếu đã tham gia)
    @Data
    public static class Response {
        private String id;
        private String title;
        private String description;
        private Integer durationDays;
        private String level;
<<<<<<< HEAD
        private String imageUrl;
        
        // Thông tin riêng của User (nếu có)
        private boolean isJoined;
        private Integer daysCompleted;
=======

        private boolean joined; // Đã tham gia chưa?
        private Integer daysCompleted; // Đã làm được bao nhiêu ngày
        private Integer progressPercent; // % hoàn thành (0-100)
>>>>>>> 3fca325 (thêm api check in challenge cho member)
        private String status;
    }
}