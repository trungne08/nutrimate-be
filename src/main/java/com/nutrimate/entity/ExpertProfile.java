package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "`Expert_Profiles`")
@Data
public class ExpertProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "expert_id")
    private String id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String specialization; // PT, Nutritionist...
    private String certification;  // URL ảnh bằng cấp
    private String bio;
    
    @Column(name = "years_experience")
    private Integer yearsExperience;

    private Float rating;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;
}