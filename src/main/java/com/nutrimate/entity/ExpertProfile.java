package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.math.BigDecimal;

@Entity
@Table(name = "`Expert_Profiles`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpertProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "expert_id") // Bạn đang dùng tên này
    private String id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private User user;

    private String specialization; 
    private String certification;  // URL ảnh bằng cấp (Bạn đang dùng tên này)
    private String bio;
    
    @Column(name = "years_experience")
    private Integer yearsExperience;

    private Float rating;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    // 👇 THÊM ĐOẠN NÀY VÀO CUỐI FILE 👇
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING; // Mặc định là Chờ duyệt

    public enum ApprovalStatus {
        PENDING,  // Chờ duyệt
        APPROVED, // Đã duyệt
        REJECTED  // Từ chối
    }
}