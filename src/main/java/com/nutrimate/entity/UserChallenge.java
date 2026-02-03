package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "`User_Challenges`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserChallenge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uc_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Column(name = "days_completed", nullable = false)
    private Integer daysCompleted = 0; // Tiến độ (VD: Đã hoàn thành 3/7 ngày)

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status;

    public enum ChallengeStatus { IN_PROGRESS, COMPLETED, FAILED }
}