package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "Health_Metric_History")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetricHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id")
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "weight_kg")
    private Float weightKg;

    @Column(name = "height_cm")
    private Float heightCm;

    private Float bmi;

    @Column(name = "recorded_at")
    private LocalDate recordedAt;
}