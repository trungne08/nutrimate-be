package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "User_Challenges")
@Data
public class UserChallenge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uc_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status;

    public enum ChallengeStatus {
        IN_PROGRESS("In Progress"),
        COMPLETED("Completed"),
        FAILED("Failed");
        
        private String value;
        ChallengeStatus(String value) { this.value = value; }
    }
}