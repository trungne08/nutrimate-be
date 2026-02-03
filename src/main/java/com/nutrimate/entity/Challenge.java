package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "`Challenges`") // Dùng backtick để match đúng bảng Challenges trong MySQL, tránh bị hạ thành 'challenges'
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Challenge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "challenge_id")
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;
    
    @Column(name = "duration_days")
    private Integer durationDays; // VD: 7 ngày, 30 ngày
    
    @Enumerated(EnumType.STRING)
    private ChallengeLevel level; // EASY, MEDIUM, HARD

    @Column(name = "image_url", length = 512)
    private String imageUrl; // URL ảnh thử thách (upload lên Cloudinary)

    public enum ChallengeLevel { EASY, MEDIUM, HARD }
}