package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "`Challenges`") // Dùng backtick để match đúng bảng Challenges trong MySQL, tránh bị hạ thành 'challenges'
@Data
public class Challenge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "challenge_id")
    private String id;

    private String title;
    private String description;
    
    @Column(name = "duration_days")
    private Integer durationDays; // VD: 7 ngày, 30 ngày
    
    @Enumerated(EnumType.STRING)
    private ChallengeLevel level; // EASY, MEDIUM, HARD

    public enum ChallengeLevel { EASY, MEDIUM, HARD }
}