package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDate;

@Entity
@Table(name = "`Health_Profiles`") // 👈 Dùng backtick để giữ nguyên chữ hoa trong MySQL
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "profile_id", length = 36) // 👈 UUID string (36 ký tự) - QUAN TRỌNG: phải có length
    private String id;
    
    @Column(name = "user_id", nullable = false, length = 36) // 👈 QUAN TRỌNG: phải có length
    private String userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('Male', 'Female', 'Other')")
    private Gender gender;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "height_cm")
    private Float heightCm;
    
    @Column(name = "weight_kg")
    private Float weightKg;
    
    @Column(name = "target_weight_kg")
    private Float targetWeightKg;
    
    @Column(name = "activity_level", length = 50)
    private String activityLevel; // Lưu dạng string để match với ENUM có space
    
    @Column(name = "dietary_preference", length = 50)
    private String dietaryPreference = "Clean Eating"; // Lưu dạng string để match với ENUM có space
    
    // Helper methods để convert enum sang string cho database
    public void setActivityLevelFromEnum(ActivityLevel level) {
        if (level != null) {
            this.activityLevel = level.getDbValue();
        }
    }
    
    public ActivityLevel getActivityLevelAsEnum() {
        if (activityLevel == null) return null;
        return ActivityLevel.fromDbValue(activityLevel);
    }
    
    public void setDietaryPreferenceFromEnum(DietaryPreference preference) {
        if (preference != null) {
            this.dietaryPreference = preference.getDbValue();
        }
    }
    
    public DietaryPreference getDietaryPreferenceAsEnum() {
        if (dietaryPreference == null) return DietaryPreference.CLEAN_EATING;
        return DietaryPreference.fromDbValue(dietaryPreference);
    }
    
    public enum Gender {
        Male, Female, Other
    }
    
    public enum ActivityLevel {
        SEDENTARY("Sedentary"),
        LIGHTLY_ACTIVE("Lightly Active"),
        MODERATELY_ACTIVE("Moderately Active"),
        VERY_ACTIVE("Very Active");
        
        private final String dbValue;
        
        ActivityLevel(String dbValue) {
            this.dbValue = dbValue;
        }
        
        public String getDbValue() {
            return dbValue;
        }
        
        public static ActivityLevel fromDbValue(String dbValue) {
            for (ActivityLevel level : values()) {
                if (level.dbValue.equals(dbValue)) {
                    return level;
                }
            }
            throw new IllegalArgumentException("Unknown activity level: " + dbValue);
        }
    }
    
    public enum DietaryPreference {
        NONE("None"),
        VEGAN("Vegan"),
        KETO("Keto"),
        CLEAN_EATING("Clean Eating"),
        BALANCED("Balanced");
        
        private final String dbValue;
        
        DietaryPreference(String dbValue) {
            this.dbValue = dbValue;
        }
        
        public String getDbValue() {
            return dbValue;
        }
        
        public static DietaryPreference fromDbValue(String dbValue) {
            for (DietaryPreference preference : values()) {
                if (preference.dbValue.equals(dbValue)) {
                    return preference;
                }
            }
            return CLEAN_EATING; // Default
        }
    }
    
    // Helper method để tính BMI
    public Float calculateBMI() {
        if (heightCm == null || heightCm <= 0 || weightKg == null || weightKg <= 0) {
            return null;
        }
        float heightInMeters = heightCm / 100f;
        return weightKg / (heightInMeters * heightInMeters);
    }
    
    // Helper method để tính tuổi
    public Integer calculateAge() {
        if (dateOfBirth == null) {
            return null;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }
}
