package com.nutrimate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
// Không dùng @Schema ở class level khi có MultipartFile để tránh lỗi Swagger
public class UpdateProfileRequest {
    
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    @Schema(description = "User's full name", example = "Nguyễn Văn A", maxLength = 100)
    private String fullName;
    
    @Size(max = 100, message = "Username must not exceed 100 characters")
    @Schema(description = "Username", example = "nguyenvana", maxLength = 100)
    private String username;
    
    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    @Schema(description = "Phone number", example = "+84901234567", maxLength = 15)
    private String phoneNumber;
    
    // MultipartFile không cần @Schema vì Swagger tự động detect cho multipart/form-data
    private MultipartFile avatarFile;
    
    @Size(max = 255, message = "Avatar URL must not exceed 255 characters")
    @Schema(description = "Avatar image URL (optional, used only if avatarFile is not provided)", example = "https://example.com/avatar.jpg", maxLength = 255)
    private String avatarUrl;
}
