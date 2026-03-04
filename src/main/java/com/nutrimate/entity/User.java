package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "`Users`") // 👈 Dùng backtick để giữ nguyên chữ hoa trong MySQL
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", length = 36) // 👈 UUID string (36 ký tự) - QUAN TRỌNG: phải có length
    private String id;
    
    @Column(name = "cognito_id", unique = true, length = 255)
    private String cognitoId; // Lưu 'sub' từ AWS Cognito
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(name = "full_name", length = 100)
    private String fullName; // Lấy từ Cognito attribute 'name'
    
    @Column(length = 100)
    private String username; // Lấy từ Cognito 'preferred_username' hoặc 'name'
    
    @Column(name = "phone_number", length = 15)
    private String phoneNumber; // Lấy từ Cognito attribute 'phone_number'
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('MEMBER', 'EXPERT', 'ADMIN') NOT NULL DEFAULT 'MEMBER'")
    private UserRole role = UserRole.MEMBER;
    
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl; // Lấy từ Cognito attribute 'picture'
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum UserRole {
        MEMBER, EXPERT, ADMIN
    }
}
