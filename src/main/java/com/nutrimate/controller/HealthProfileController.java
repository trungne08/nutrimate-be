package com.nutrimate.controller;

import com.nutrimate.dto.HealthProfileRequest;
import com.nutrimate.entity.HealthProfile;
import com.nutrimate.entity.User;
import com.nutrimate.repository.HealthProfileRepository;
import com.nutrimate.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Health Profile", description = "API endpoints for managing user health profiles")
public class HealthProfileController {
    
    private final HealthProfileRepository healthProfileRepository;
    private final UserRepository userRepository;
    
    public HealthProfileController(HealthProfileRepository healthProfileRepository,
                                  UserRepository userRepository) {
        this.healthProfileRepository = healthProfileRepository;
        this.userRepository = userRepository;
    }
    
    @Operation(
            summary = "Create or update health profile",
            description = "Creates a new health profile or updates existing one for the authenticated user. " +
                         "Calculates BMI and age automatically. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Health profile saved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"success\": true, \"message\": \"Health profile saved successfully\", \"healthProfile\": {\"id\": 1, \"gender\": \"Male\", \"dateOfBirth\": \"1990-01-15\", \"heightCm\": 170.0, \"weightKg\": 70.0, \"targetWeightKg\": 65.0, \"activityLevel\": \"Moderately Active\", \"dietaryPreference\": \"Clean Eating\", \"bmi\": 24.22, \"age\": 34}}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Validation error"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            )
    })
    @PostMapping("/profile")
    public ResponseEntity<Map<String, Object>> saveHealthProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User,
            @Valid @RequestBody HealthProfileRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (oidcUser == null && oauth2User == null) {
            response.put("success", false);
            response.put("message", "Unauthorized - Please login first");
            return ResponseEntity.status(401).body(response);
        }
        
        // Lấy email từ authenticated user
        String email = oidcUser != null ? oidcUser.getEmail() : oauth2User.getAttribute("email");
        
        // Tìm user trong database
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found in database");
            return ResponseEntity.status(404).body(response);
        }
        
        User user = userOpt.get();
        
        // Kiểm tra xem đã có health profile chưa
        Optional<HealthProfile> existingProfileOpt = healthProfileRepository.findByUserId(user.getId());
        
        HealthProfile healthProfile;
        boolean isNew = existingProfileOpt.isEmpty();
        
        if (isNew) {
            // Tạo mới
            healthProfile = new HealthProfile();
            healthProfile.setUserId(user.getId());
        } else {
            // Cập nhật
            healthProfile = existingProfileOpt.get();
        }
        
        // Cập nhật thông tin (các trường bắt buộc)
        healthProfile.setGender(request.getGender());
        healthProfile.setDateOfBirth(request.getDateOfBirth());
        healthProfile.setHeightCm(request.getHeightCm());
        healthProfile.setWeightKg(request.getWeightKg());
        
        // Các trường optional (chỉ cập nhật nếu có giá trị)
        if (request.getTargetWeightKg() != null) {
            healthProfile.setTargetWeightKg(request.getTargetWeightKg());
        }
        if (request.getActivityLevel() != null) {
            healthProfile.setActivityLevelFromEnum(request.getActivityLevel());
        }
        if (request.getDietaryPreference() != null) {
            healthProfile.setDietaryPreferenceFromEnum(request.getDietaryPreference());
        } else if (isNew) {
            // Nếu tạo mới và không có dietaryPreference, set default
            healthProfile.setDietaryPreferenceFromEnum(HealthProfile.DietaryPreference.CLEAN_EATING);
        }
        
        // Lưu vào database
        try {
            healthProfile = healthProfileRepository.save(healthProfile);
            
            // Tính toán BMI và age
            Float bmi = healthProfile.calculateBMI();
            Integer age = healthProfile.calculateAge();
            
            // Trả về thông tin health profile
            Map<String, Object> profileInfo = new HashMap<>();
            profileInfo.put("id", healthProfile.getId());
            profileInfo.put("gender", healthProfile.getGender().name());
            profileInfo.put("dateOfBirth", healthProfile.getDateOfBirth());
            profileInfo.put("heightCm", healthProfile.getHeightCm());
            profileInfo.put("weightKg", healthProfile.getWeightKg());
            profileInfo.put("targetWeightKg", healthProfile.getTargetWeightKg());
            profileInfo.put("activityLevel", healthProfile.getActivityLevel());
            profileInfo.put("dietaryPreference", healthProfile.getDietaryPreference());
            profileInfo.put("bmi", bmi != null ? Math.round(bmi * 100.0) / 100.0 : null);
            profileInfo.put("age", age);
            
            response.put("success", true);
            response.put("message", isNew ? "Health profile created successfully" : "Health profile updated successfully");
            response.put("healthProfile", profileInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error saving health profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @Operation(
            summary = "Get health profile",
            description = "Returns the authenticated user's health profile with calculated BMI and age. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Health profile retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Health profile not found"
            )
    })
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getHealthProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (oidcUser == null && oauth2User == null) {
            response.put("success", false);
            response.put("message", "Unauthorized - Please login first");
            return ResponseEntity.status(401).body(response);
        }
        
        // Lấy email từ authenticated user
        String email = oidcUser != null ? oidcUser.getEmail() : oauth2User.getAttribute("email");
        
        // Tìm user trong database
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found in database");
            return ResponseEntity.status(404).body(response);
        }
        
        User user = userOpt.get();
        
        // Tìm health profile
        Optional<HealthProfile> healthProfileOpt = healthProfileRepository.findByUserId(user.getId());
        
        if (healthProfileOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Health profile not found. Please create one first.");
            return ResponseEntity.status(404).body(response);
        }
        
        HealthProfile healthProfile = healthProfileOpt.get();
        
        // Tính toán BMI và age
        Float bmi = healthProfile.calculateBMI();
        Integer age = healthProfile.calculateAge();
        
        // Trả về thông tin health profile
        Map<String, Object> profileInfo = new HashMap<>();
        profileInfo.put("id", healthProfile.getId());
        profileInfo.put("gender", healthProfile.getGender().name());
        profileInfo.put("dateOfBirth", healthProfile.getDateOfBirth());
        profileInfo.put("heightCm", healthProfile.getHeightCm());
        profileInfo.put("weightKg", healthProfile.getWeightKg());
        profileInfo.put("targetWeightKg", healthProfile.getTargetWeightKg());
        profileInfo.put("activityLevel", healthProfile.getActivityLevel());
        profileInfo.put("dietaryPreference", healthProfile.getDietaryPreference());
        profileInfo.put("bmi", bmi != null ? Math.round(bmi * 100.0) / 100.0 : null);
        profileInfo.put("age", age);
        
        response.put("success", true);
        response.put("healthProfile", profileInfo);
        
        return ResponseEntity.ok(response);
    }
}
