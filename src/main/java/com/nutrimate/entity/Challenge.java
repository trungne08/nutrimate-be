package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Challenges")
@Data
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "challenge_id")
    private String id;

    private String title;
    private String description;
    
    @Column(name = "duration_days")
    private Integer durationDays;
}