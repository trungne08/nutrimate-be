package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CheckIn_Logs")
@Getter
@Setter
@NoArgsConstructor
public class CheckInLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_challenge_id")
    private UserChallenge userChallenge;

    private LocalDate checkinDate;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}