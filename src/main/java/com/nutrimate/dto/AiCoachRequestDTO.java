package com.nutrimate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO gửi sang Python AI Microservice (http://localhost:8000/api/ai/chat).
 * JSON: user_message, health_profile, available_recipes (snake_case theo Python).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCoachRequestDTO {
    @JsonProperty("user_message")
    private String userMessage;

    @JsonProperty("health_profile")
    private HealthProfileDTO healthProfile;

    @JsonProperty("available_recipes")
    private List<AvailableRecipeDTO> availableRecipes;

    @JsonProperty("chat_history")
    private List<ChatMessageDTO> chatHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageDTO {
        private String role;
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthProfileDTO {
        private String gender;

        @JsonProperty("height_cm")
        private Float heightCm;

        @JsonProperty("weight_kg")
        private Float weightKg;

        @JsonProperty("target_weight_kg")
        private Float targetWeightKg;

        @JsonProperty("activity_level")
        private String activityLevel;

        @JsonProperty("dietary_preference")
        private String dietaryPreference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableRecipeDTO {
        @JsonProperty("recipe_name")
        private String recipeName;

        private Integer calories;
        private String description;
    }
}
