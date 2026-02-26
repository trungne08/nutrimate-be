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
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/health")
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
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User,
            @Valid @RequestBody HealthProfileRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 1. TÓM ĐỊNH DANH TỪ MỌI NGUỒN (Session hoặc Bearer Token)
        String email = null;
        String cognitoId = null;

        if (oidcUser != null) {
            email = oidcUser.getEmail();
            cognitoId = oidcUser.getSubject();
        } else if (oauth2User != null) {
            email = oauth2User.getAttribute("email");
        } else if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            email = jwt.getClaimAsString("email");
            cognitoId = jwt.getSubject(); // Lấy "sub"
            if (cognitoId == null) cognitoId = jwt.getClaimAsString("username");
        }

        // 2. NẾU KHÔNG CÓ GÌ -> CHẶN LẠI
        if ((email == null || email.trim().isEmpty()) && (cognitoId == null || cognitoId.trim().isEmpty())) {
            response.put("success", false);
            response.put("message", "Unauthorized - Please login first");
            return ResponseEntity.status(401).body(response);
        }
        
        // 3. TÌM USER TRONG DATABASE BẰNG EMAIL HOẶC COGNITO ID
        Optional<User> userOpt = Optional.empty();
        if (email != null && !email.trim().isEmpty()) {
            userOpt = userRepository.findByEmail(email);
        }
        if (userOpt.isEmpty() && cognitoId != null && !cognitoId.trim().isEmpty()) {
            userOpt = userRepository.findByCognitoId(cognitoId);
        }
        
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found in database");
            return ResponseEntity.status(404).body(response);
        }
        
        User user = userOpt.get();
        
        // --- PHẦN LOGIC LƯU PROFILE CŨ CỦA BẠN GIỮ NGUYÊN ---
        Optional<HealthProfile> existingProfileOpt = healthProfileRepository.findByUserId(user.getId());
        HealthProfile healthProfile;
        boolean isNew = existingProfileOpt.isEmpty();
        
        if (isNew) {
            healthProfile = new HealthProfile();
            healthProfile.setUserId(user.getId());
        } else {
            healthProfile = existingProfileOpt.get();
        }
        
        healthProfile.setGender(request.getGender());
        healthProfile.setDateOfBirth(request.getDateOfBirth());
        healthProfile.setHeightCm(request.getHeightCm());
        healthProfile.setWeightKg(request.getWeightKg());
        
        if (request.getTargetWeightKg() != null) healthProfile.setTargetWeightKg(request.getTargetWeightKg());
        if (request.getActivityLevel() != null) healthProfile.setActivityLevelFromEnum(request.getActivityLevel());
        if (request.getDietaryPreference() != null) {
            healthProfile.setDietaryPreferenceFromEnum(request.getDietaryPreference());
        } else if (isNew) {
            healthProfile.setDietaryPreferenceFromEnum(HealthProfile.DietaryPreference.CLEAN_EATING);
        }
        
        try {
            healthProfile = healthProfileRepository.save(healthProfile);
            Float bmi = healthProfile.calculateBMI();
            Integer age = healthProfile.calculateAge();
            
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
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 1. TÓM ĐỊNH DANH TỪ MỌI NGUỒN
        String email = null;
        String cognitoId = null;

        if (oidcUser != null) {
            email = oidcUser.getEmail();
            cognitoId = oidcUser.getSubject();
        } else if (oauth2User != null) {
            email = oauth2User.getAttribute("email");
        } else if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            email = jwt.getClaimAsString("email");
            cognitoId = jwt.getSubject(); // Lấy "sub"
            if (cognitoId == null) cognitoId = jwt.getClaimAsString("username");
        }

        if ((email == null || email.trim().isEmpty()) && (cognitoId == null || cognitoId.trim().isEmpty())) {
            response.put("success", false);
            response.put("message", "Unauthorized - Please login first");
            return ResponseEntity.status(401).body(response);
        }
        
        // 2. TÌM USER TRONG DATABASE
        Optional<User> userOpt = Optional.empty();
        if (email != null && !email.trim().isEmpty()) {
            userOpt = userRepository.findByEmail(email);
        }
        if (userOpt.isEmpty() && cognitoId != null && !cognitoId.trim().isEmpty()) {
            userOpt = userRepository.findByCognitoId(cognitoId);
        }
        
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found in database");
            return ResponseEntity.status(404).body(response);
        }
        
        User user = userOpt.get();
        
        // --- PHẦN LOGIC GET PROFILE CŨ GIỮ NGUYÊN ---
        Optional<HealthProfile> healthProfileOpt = healthProfileRepository.findByUserId(user.getId());
        
        if (healthProfileOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Health profile not found. Please create one first.");
            return ResponseEntity.status(404).body(response);
        }
        
        HealthProfile healthProfile = healthProfileOpt.get();
        Float bmi = healthProfile.calculateBMI();
        Integer age = healthProfile.calculateAge();
        
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
